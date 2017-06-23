package com.elastic.barretta.collectors.news

import groovy.util.logging.Slf4j
import wslite.http.auth.HTTPBasicAuthorization
import wslite.rest.RESTClient
import wslite.rest.RESTClientException

/**
 * lightweight ES client
 */
@Slf4j
class ESClient {

    @Delegate
    private RESTClient client
    private Config config

    static class Config {
        String url
        String index
        String type = "doc"
        String user
        String pass

        @Override
        public String toString() {
            return "Config{" +
                "\nurl [" + url + ']' +
                "\nindex [" + index + ']' +
                "\ntype [" + type + ']' +
                "\nuser [" + user + ']' +
                "\npass [" + pass + ']' +
                '\n}'
        }
    }

    ESClient(Config config) {
        this.client = new RESTClient(config.url)
        this.config = config
        if (config.user) {
            client.authorization = new HTTPBasicAuthorization(config.user, config.pass)
        }
        testClient()
    }

    def deleteIndex(String index = config.index) {
        log.info("deleting index [$config.url/$index]")
        client.delete(path: "/$index")
    }

    def createIndex(Map mapping = null, String index = config.index) {
        if (!indexExists()) {
            log.info("creating index [$config.url/$index]")
            client.put(path: "/$index") {
                if (mapping) {
                    json mappings: mapping
                }
            }
        } else {
            log.warn("index [$config.index] already exists")
        }
    }

    def indexExists(index = config.index) {
        def exists = true
        try {
            client.head(path: "/$index")
        } catch (RESTClientException e) {
            if (e.response.statusCode == 404) {
                exists = false
            }
        }
        return exists
    }

    def putDoc(Map content, index = config.index, type = config.type) {
        client.put(path: "/$index/$type") {
            json content
        }
    }

    def updateDoc(id, Map content, index = config.index, type = config.type) {
        client.put(path:"/$index/$type/$id") {
            json content
        }
    }

    def docExists(String field, String value, String index = config.index, String type = config.type) {
        def response = client.post(path:"/$index/$type/_search") {
            json size: 0, query: [ match: [ (field): value ] ]
        }
        return response.json.hits.total > 0
    }

    def getDocByUniqueField(String uniqueField, String value, String index = config.index, String type = config.type) {
        def response = client.post(path:"/$index/$type/_search") {
            json size: 1, query: [ match: [ (uniqueField): value ] ]
        }
        return response.json.hits.hits[0]
    }

    private testClient() {
        try {
            client.get()
        } catch (RESTClientException e) {
            log.error("unable to connect to ES [${e.getMessage()}]\n$config")
            System.exit(1)
        }
    }
}