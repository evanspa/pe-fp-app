(ns pe-fp-app.endpoint
  (:require [compojure.core :refer [routes]]
            [liberator.dev :refer [wrap-trace]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.handler :as handler]
            [pe-fp-app.core :refer [fp-route-definitions]]))

(def fp-routes (apply routes fp-route-definitions))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Middleware-decorated app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def fp-app
  (-> fp-routes
      (handler/api)
      ;(wrap-trace :header)
      (wrap-params)
      (wrap-cookies)))
