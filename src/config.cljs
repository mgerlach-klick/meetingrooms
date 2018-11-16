(ns config
  (:require ["aws-sdk" :as AWS]))


(def env {:region          "us-east-1"
          :rooms-file      "rooms.edn"
          :bucket-name     "klick-meetingrooms-anonymous"
          :cognito-pool-id "us-east-1:df130540-5cda-4432-aa21-bf1e325f493d"
          :s3url           "https://s3.amazonaws.com/klick-meetingrooms-anonymous/pics/"})


(def aws-config {:region      (env :region)
                 :credentials (AWS/CognitoIdentityCredentials. #js {:IdentityPoolId (env :cognito-pool-id) })})
