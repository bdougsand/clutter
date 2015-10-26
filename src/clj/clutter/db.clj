(ns clutter.db
  (:require [clojure.core.async :as async]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.walk :as walk]

            [cemerick.friend.credentials :as creds]

            [taoensso.timbre :refer [info]]

            [clutter.changes :refer [apply-change merge-changes]]
            [clutter.query :as query]
            [clutter.utils :refer [ref?]])
  (:refer-clojure :exclude [name]))

(remove-method print-method clojure.lang.IDeref)

(def dbtop
  "The last used dbid."
  (ref 0))

(def db
  (ref {}))

(def users
  "A mapping of username -> dbref"
  (ref {}))

(defn db-get
  [id]
  (get @db id))

(defn ->dbref [x]
  (cond
   (ref? x) x
   :else (db-get x)))

(defn ->dbid [x]
  (if (ref? x) (:id @x) x))



(declare db-summary)
(defn query
  [q]
  (query/query db-summary @db q))


(defn push-changes!
  "Apply changesets to objects in the database. db-changes should be a
  sequence of [dbid change] pairs, where change conforms to the format
  described in clutter.changes."
  [db-changes]
  (info "Pushing db changes:" db-changes)
  (dosync
   (doseq [[dbid change] db-changes
           :let [what (->dbref dbid)]]
     (if what
       (alter what apply-change change)

       (throw (Throwable. (str "Object does not exist with id " dbid)))))))

(declare connections contents location neighbors neighbors-inclusive)
(defn subscribers
  "Returns a set containing the references of the subscribers to
  what--i.e., the dbrefs that will be informed when a change is made to
  what."
  [what]
  (disj (set (filter connections
                     (concat
                      (contents what)
                      ;; Include what (receive information about own updates)
                      (neighbors-inclusive what)

                      ;; TODO: allow manual subscribers?
                      #_(:subscribers @what))))
        nil))

(declare push-and-publish-changes!)
(defn delete!
  [what]
  (dosync
   (push-and-publish-changes!
    [[(:id @what) {:location nil}]
     (when-let [loc (:location @what)]
       [(:id @loc) {:contents {:$rem [what]}}])])
   (alter db dissoc (:id @what))))

(defn auto-subscriptions
  "Returns a set of references to the dbrefs that what will
  automatically receive information about: i.e., its location, itself,
  and its neighbors."
  [what]
  (disj (set (list* (location what) what (neighbors what))) nil))

;; NOTE: Manual subscriptions are not yet running
(defn subscribe!
  "Manually add a subscriber to the dbref pub-what."
  [pub-what sub-what]
  (dosync
   (alter pub-what (fn [w]
                     (assoc w :subscribers (cons sub-what (:subscribers w)))))))

(defn unsubscribe!
  [pub-what sub-what]
  (dosync
   (alter pub-what (fn [w]
                     (assoc w :subscribers
                            (remove (partial = sub-what) (:subscribers w)))))))

(defn flatten-change
  "Removes refs from the change map. Returns a vector of [refs
  change]. Run this before forwarding changes to the client."
  [change]
  (let [refs (atom #{})
        done (walk/postwalk
              (fn [x]
                (if (ref? x)
                  (do
                    (swap! refs conj x)
                    (:id @x))
                  x))
              change)]
    [refs done]))

(defn replace-dbrefs
  "Replaces db-references with their ids, since we can't send refs to
  clients."
  [form]
  (walk/postwalk (fn [x] (if (ref? x) (:id @x) x)) form))

(defn subscriber-changes
  "Takes a sequence of [dbid dbref-change]. Returns a map of [subscriber
  db-change]. Does not make any actual changes to the database."
  [db-changes]
  (let [dbmap @db]
    (apply merge-with (partial merge-with merge-changes)
           (map (fn [[dbid ch]]
                  (when-let [dbref (get dbmap dbid)]
                    (zipmap (subscribers dbref) (repeat {dbid (replace-dbrefs ch)}))))
                db-changes))))

(declare tell)
(defn push-and-publish-changes!
  "Applies a changeset to the database, then publishes the changes to
  all subscribers."
  [db-changes]
  (let [db-changes (remove nil? db-changes)]
    (push-changes! db-changes)
    (doseq [[sub-ref changes] (subscriber-changes db-changes)]
      ;; TODO: Add this to the next outgoing message, if the delay is
      ;; tolerable.
      (tell sub-ref {:dbd changes}))))


(declare location set-location!)
(defn make-new
  [m & {:keys [where]}]
  (dosync
   (let [objid (alter dbtop inc)
         objref (ref (assoc m :id objid))]
     (alter db assoc objid objref)
     (when-let [where (or where (db-get 0))]
       (set-location! objref where))

     [objid objref])))

(defn get-user
  [name]
  (get @users (str/lower-case name)))

(declare id do-tell tell-all)
(defn set-location!
  [what where]
  (let [where-id (id where)
        what-id (id what)
        last-where (:location @what)]
    (when-not (= where last-where)
      (dosync
      (push-and-publish-changes!
       (concat
        [[where-id {:contents {:$add [what]}}]
         [what-id {:location where}]]
        (when last-where
          [[(id last-where) {:contents {:$rem [what]}}]])))

      ;; The right way to do this is to calculate how the change affects
      ;; subscriptions, then to push db information to the appropriate
      ;; clients. E.g., `what' should get a db push about its neighbors
      ;; in the new location and the location. The neighbors and location
      ;; should receive information about the newcomer.

      ;; For now, do it manually:

      ;; Inform what:
      (tell what {:dbd (into {} (map db-summary) (list* where (contents where)))})

      ;; Inform neighbors:
      (tell-all (disj (set (cons where (contents where))) what)
                {:dbd (db-summary what)})))))

(defn make-user [name password]
  (dosync
   (let [[uid uref] (make-new {:type :user
                               :name name
                               :password (creds/hash-bcrypt password)})]
     (alter users assoc (str/lower-case name) uref)
     uref)))

(defn get-or-make-user
  "Returns [new? uref] where new? is true if the user was newly
  created. id and ref are the dbid and ref of the user."
  [name password]
  (if-let [uref (get-user name)]
    [false uref]
    [true (make-user name password)]))

(defn authorize-or-create-user*
  [{:keys [username password]}]
  (info "LOGIN:" username "with password:" password)
  (if-let [u (get-user username)]
    (when (creds/bcrypt-verify password (:password @u))
      {:type :user
       :name (:name @u)})

    (do
      @(make-user username password)
      {:type :user
       :name username})))

(defn authorize-or-create-user
  [x]
  (let [u (authorize-or-create-user* x)]
    (info "USER AUTHORIZED:" u)
    u))

(defn set-password!
  [username pw]
  (when-let [u (get-user username)]
    (dosync
     (alter u assoc :password (creds/hash-bcrypt pw)))))

(defn contents
  "Contents is a list of refs."
  [what]
  (:contents @what))

(defn neighbors-inclusive [what]
  (when-let [l (location what)]
    (set (contents l))))

(defn neighbors [what]
  (when-let [l (location what)]
    (disj (set (contents l)) #{what})))

(defn name
  [what]
  (:name @what))

(defn dbtype
  [what]
  (:type @what))

(defn id
  [what]
  (:id @what))

(defn location
  [what]
  (:location @what))

(declare prop-summary)

(defmulti type-summary (comp :type deref))

(defmethod type-summary :user
  [u]
  {:online (pos? (count (:connections @u)))})
(defmethod type-summary :default [_] nil)


(defn db-summary
  "Returns a mapping of id -> map."
  [what]
  {(id what) (merge {:name (name what)
                     :type (:type @what)
                     :id (id what)
                     :location (when-let [l (location what)] (id l))
                     :props (prop-summary what)
                     :contents (map id (contents what))}
                    (type-summary what))})

(defn auto-subscribed-summary
  "Returns a db map containing summaries of the objects to which what is
  auto-subscribed: its neighbors, location, and contents."
  [what]
  (into {} (map db-summary (auto-subscriptions what))))

;; Connection management and messaging:
(defn add-connection!
  [what c]
  (dosync
   (let [cons-count (count (:connections (alter what #(assoc % :connections (cons c (:connections %))))))]
     (when (> 1 cons-count)
       (when-let [where (location what)]
         (tell-all (subscribers what)
                   {:dbd {(id what) {:online true}}}))))))

(defn remove-connection!
  [what c]
  (dosync
   (alter what #(assoc % :connections (remove (partial = c) (:connections %))))))

(defn connections
  [what]
  (:connections @what))

(defn do-tell
  [what m]
  (when-let [what @what]
    (let [conns (:connections what)]
      (doseq [conn conns]
        (async/put! conn m)))))
(def tell do-tell)

#_(defmacro tell
  "Send a message to all connections attached to the object what. If
  what is not connected, the message m will not be evaluated."
  [what m]
  `(when (seq (connections ~what))
     (do-tell ~what ~m)))

(defn tell-all [whats m]
  (doseq [what whats]
    (do-tell what m)))

(defn notify
  "Notify the contents of an object."
  [where m]
  (doseq [what (contents where)]
    (tell what m)))

(defn notify-except
  "exclude should be a set of refs to exclude."
  [where m exclude]
  (doseq [what (contents where)]
    (when-not (contains? exclude what)
      (tell what m))))

(defn otell
  "Send a message to everyone in what's location."
  [what m]
  (notify-except (location what) m #{what}))

(defn otell-all
  [what m]
  (notify (location what) m))

(defn who
  "Returns a sequence of the users currently connected."
  []
  (filter (fn [u]
            (pos? (count (:connections @u))))
          (map val @users)))


;; Properties:
;; Properties can be strings or maps.
;; Map:
;;  { :string  ; the string contents of the property
;;    :code    ; string
;;    :publish ; boolean
;;    :public  ; boolean
;;  }
(defn props
  "Returns the dbref's properties."
  [what]
  (:props @what))

(defn get-prop
  [what prop]
  (some-> what props (get prop)))

(defn desc [what]
  (or (get-prop what :description)
      "You see nothing special."))

(defn publish?
  "Should changes to this property be pushed to unprivileged watchers?"
  [prop]
  (or (string? prop)
      (:publish prop)))

(defn public?
  "Can the property be viewed by unprivileged users?"
  [prop]
  (or (string? prop)
      (:public prop)
      (:publish prop)))

(defn prop-summary
  "Returns a map containing the properties of the dbref marked for
  publication."
  [what]
  (into {} (filter (comp publish? val) (props what))))

(defn query-with-perms
  "Like (query), but removes any results that the user should not be
  able to see."
  [user q]
  ;; TODO: Implement  <.<
  (query q))

(defn set-prop-string!
  "Set a property's string value and push it to subscribers."
  ([what pname pval opts]
     (let [prop (merge {:string (str pval)} opts)]
       (push-and-publish-changes!
        {(->dbid what) {:props {pname prop}}})))
  ([what pname pval]
   (set-prop-string! what pname pval nil)))

(defn set-name!
  [what new-name]
  (dosync
   (push-and-publish-changes!
    [(id what) {:name new-name}])))

;; Using
(defn can-use?
  [who what]
  (or (= (location who) (location what))
      (= (location what) who)))

(defn can-use-id?
  [who what-id]
  (can-use? who (db-get what-id)))
