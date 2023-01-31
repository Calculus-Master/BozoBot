package com.calculusmaster.bozo.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Mongo
{
    private static final ConnectionString CONNECT_MAIN = new ConnectionString(HiddenConfig.MONGO_USER);
    private static final MongoClient CLIENT_MAIN = MongoClients.create(MongoClientSettings.builder().applyConnectionString(CONNECT_MAIN).retryReads(true).retryWrites(true).build());

    private static final MongoDatabase BozoBotDB = CLIENT_MAIN.getDatabase("BozoBot");

    public static final MongoCollection<Document> QuestionsVotingDB = BozoBotDB.getCollection("QuestionsVoting");
}
