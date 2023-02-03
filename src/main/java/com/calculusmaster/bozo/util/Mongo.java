package com.calculusmaster.bozo.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;

public class Mongo
{
    private static final ConnectionString CONNECT_MAIN = new ConnectionString(HiddenConfig.MONGO_USER);
    private static final MongoClient CLIENT_MAIN = MongoClients.create(MongoClientSettings.builder().applyConnectionString(CONNECT_MAIN).retryReads(true).retryWrites(true).build());

    private static final MongoDatabase BozoBotDB = CLIENT_MAIN.getDatabase("BozoBot");

    public static final MongoCollection<Document> QuestionsVotingDB = BozoBotDB.getCollection("QuestionsVoting");
    public static final MongoCollection<Document> UserMomentsDB = BozoBotDB.getCollection("UserMoments");

    public static void main(String[] args)
    {
        Mongo.UserMomentsDB.insertOne(new Document("type", "balti").append("attachments", new ArrayList<>()).append("queued", new ArrayList<>()));
        Mongo.UserMomentsDB.insertOne(new Document("type", "joyboy").append("attachments", new ArrayList<>()).append("queued", new ArrayList<>()));
        Mongo.UserMomentsDB.insertOne(new Document("type", "handsome").append("attachments", new ArrayList<>()).append("queued", new ArrayList<>()));
        Mongo.UserMomentsDB.insertOne(new Document("type", "stolas").append("attachments", new ArrayList<>()).append("queued", new ArrayList<>()));
    }
}
