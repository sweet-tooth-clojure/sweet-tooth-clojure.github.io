[["/todo-list"
  {::serr/ns   :sweet-tooth.todo-example.backend.endpoint.todo-list
   ::serr/type :collection
   :name       :todo-lists}]
 ["/todo-list/{id}"
  {::serr/ns   :sweet-tooth.todo-example.backend.endpoint.todo-list
   ::serr/type :member
   :name       :todo-list
   :id-key     :id}]]

(def decisions
  {:coll
   {:get  {:handle-ok (comp tl/todo-lists ed/db)}
    :post {:post!          ed/create->:result
           :handle-created ed/created-pull}}

   :ent
   {:get {:handle-ok (fn [ctx])}
    :put {:put!      ed/update->:result
          :handle-ok ed/updated-pull}

    :delete {:delete!   (fn [ctx])
             :handle-ok []}}})

[["/api/v1/todo-list"
  {::serr/ns   :project.endpoint.todo-list
   ::serr/type :collection
   :name       :todo-lists}]
 ["/api/v1/todo-list/{id}"
  {::serr/ns   :project.endpoint.todo-list
   ::serr/type :member
   :name       :todo-list
   :id-key     :id}]]

(def decisions
  {:member/todo-items
   {:get  {:handle-ok (fn [ctx])}
    :post {:handle-created (fn [ctx])}}})

(serr/expand-routes
 [{::serr/path-prefix "/api/v1"}
  [:project.endpoint.todo-list]
  [:project.endpoint.todo]])

[["/user" {:name    :users
           :handler project.endpoint.user/list}]
 ["/user{id}" {:name    :user
               :handler project.endpoint.user/show}]

 ["/todo-list" {:name    :todo-lists
                :handler project.endpoint.todo-list/list}]
 ["/todo-list/{id}" {:name    :todo-list
                     :handler project.endpoint.todo-list/show}]]

(require '[reitit.core :as r])
(r/match-by-name (r/router (serr/expand-routes [[:project.endpoint.todo-list]]))
                 :todo-lists)

(-> [[:project.endpoint.todo-list]]
    serr/expand-routes
    r/router
    (r/match-by-name
     :todo-lists)
    :path)



(defn create-todo
  [{:keys [params session] :as _req}]
  (if-not (authorized? session)
    (throw NotAuthorized "you do not have permission to do that"))
  (if-let [errors (validation-errors ::create-todo params)]
    {:status 400
     :body   {:errors errors}}
    (create-todo! params)))

(defn create-todo
  [{:keys [params session] :as _req}]
  (cond (not (authorized? session))
        {:status 401
         :body   {:errors "not authorized"}}

        (not (valid? ::create-todo params))
        {:status 400
         :body   {:errors (validation-errors ::create-todo params)}}

        :else
        (create-todo! params)))

(def create-todo
  (liberator/resource
   :authorized? el/authenticated?
   :malformed?  (el/validate-describe v/todo-rules)
   :post!       ed/create->:result
   :handle-ok   el/created-pull))

(def decision-statuses
  {:authorized? 401
   :malformed?  400
   :handle-ok   200})

(def decision-graph
  {:authorized? {true  :malformed?
                 false 401}
   :malformed?  {true  400
                 false :handle-ok}
   :handle-ok   200})

(defn decisions->handler
  [decision-nodes]
  (fn [req]
    (loop [node :authorized?]
      (let [result              ((node decision-nodes) req)
            edges-or-status     (node decision-graph)
            next-node-or-status (get edges-or-status (boolean result) edges-or-status)]
        (if (keyword? next-node-or-status)
          (recur next-node-or-status) ;; it was a node; on to the next decision!
          {:status next-node-or-status
           :body   result})))))

(defn handle-ok
  [ctx]
  (create-todo! (merge (get-in ctx [:request :params])
                       {:user-id (get-in ctx [:auth-user :id])})))

(def decision-graph
  {:authorized?         {true  :malformed?
                         false :handle-unauthorized}
   :handle-unauthorized 401
   :malformed?          {true  :handle-malformed
                         false :handle-ok}
   :handle-malformed    400
   :handle-ok           200})

(defn conform-decision-result
  [result]
  (if (vector? result)
    result
    [result {}]))

(defn decisions->handler
  [decision-nodes]
  (fn [req]
    (loop [ctx  {:request req}
           node :authorized?]
      (let [[result added-context] (conform-decision-result ((node decision-nodes) ctx))
            edges-or-status        (node decision-graph)
            next-node-or-status    (get edges-or-status (boolean result) edges-or-status)]
        (if (keyword? next-node-or-status)
          (recur (merge ctx added-context) next-node-or-status) ;; it was a node; on to the next decision!
          {:status next-node-or-status
           :body   result})))))

(def handler
  (decisions->handler
   {:authorized? (fn [ctx] [true {:auth-user {:user-id 1}}])
    :malformed?  (constantly false)
    :handle-ok   (fn [ctx] (str "Logged in as " (get-in ctx [:auth-user :user-id])))}))

(defn hi
  []
  uh oh)


(def decision-graph
  {:authorized?         {true  :malformed?
                         false :handle-unauthorized}
   :malformed?          {true  :handle-malformed
                         false :handle-ok}
   :handle-unauthorized 401
   :handle-malformed    400
   :handle-ok           200})

(defn conform-decision-result
  [result]
  (if (vector? result)
    result
    [result {}]))

(defn decisions->handler
  [decision-nodes]
  (fn [req]
    (loop [ctx  {:request req}
           node :authorized?]
      (let [edges-or-status (node decision-graph)
            node-type       (if (map? edges-or-status)
                              :decision
                              :status-handler)]
        (case node-type
          :decision       (let [[result added-context] (conform-decision-result ((node decision-nodes) ctx))
                                next-node              (get edges-or-status (boolean result))]
                      (recur (merge ctx added-context) next-node))
          :status-handler {:status edges-or-status
                           :body   ((node decision-nodes (constantly nil)) ctx)})))))

(def create-todo-list-handler
  (decisions->handler
   {:authorized?      (fn [ctx]
                        (when-let [user (get-in ctx [:request :user])]
                          [true {:user user}]))
    :malformed?       (fn [ctx]
                        (if (get-in ctx [:request :params :todo-list/title])
                          false
                          [true {:errors ["No to-do list title"]}]))
    :handle-malformed (fn [{:keys [errors]}] errors)
    :handle-ok        (fn [ctx]
                        (merge (get-in ctx [:request :params])
                               {:todo-list/owner (get-in ctx [:user :id])}))}))

(create-todo-list-handler {:user {:id 1}})
(create-todo-list-handler
 {:user   {:id 1}
  :params {:todo-list/title "write some docs this is your life now"}})


(fn [ctx]
  (d/transact! (:db ctx) [...]))


(def decisions
  {:collection
   {:get {:handle-ok (fn [ctx]
                       [{:id 1 :todo-list/title "title 1"}
                        {:id 2 :todo-list/title "title 2"}
                        {:id 1 :todo-list/title "title 3"}])}}

   :member
   {:get {:handle-ok (fn [ctx] {:id 1 :todo-list/title "title 1"})}}})

^{:ent-type :todo-list} {:id 1 :todo-list/title "title 1"}

^{:ent-type :todo-list} [{:id 1 :todo-list/title "title 1"}
                         {:id 2 :todo-list/title "title 2"}
                         {:id 1 :todo-list/title "title 3"}]

[^{:ent-type :todo-list} {:id 1 :todo-list/title "title 1"}
 ^{:ent-type :todo}      {:id 1 :todo/title "todo 1"}]


(def decisions
  {:collection
   {:get  {:handle-ok
           (fn [ctx]
             ;; this is a constant, but you would probably have a function that
             ;; returns a sequence of records from a db
             [{:id 1, :todo-list/title "to-do list"}])}

    :post {:handle-created
           (fn [{{:keys [params]} :request}]
             (db/insert! :todo-list params))}}

   :member
   {:get {:handle-ok
          (fn [ctx]
            {:id 1, :todo-list/title "to-do list"})}

    :put {:handle-ok
          (fn [{{:keys [params]} :request}]
            (db/update! :todo-list params))}

    :delete {:handle-ok
             (fn [{{:keys [params]} :request}]
               (db/delete! :todo-list (:id params)))}}})

(start-server (handler (db-connection)))
