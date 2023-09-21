package com.calculusmaster.bozo.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class Mongo
{
    private static final ConnectionString CONNECT_MAIN = new ConnectionString(HiddenConfig.MONGO_USER);
    private static final MongoClient CLIENT_MAIN = MongoClients.create(MongoClientSettings.builder().applyConnectionString(CONNECT_MAIN).retryReads(true).retryWrites(true).build());

    private static final MongoDatabase BozoBotDB = CLIENT_MAIN.getDatabase("BozoBot");

    public static final MongoCollection<Document> QuestionsVotingDB = BozoBotDB.getCollection("QuestionsVoting");
    public static final MongoCollection<Document> UserMomentsDB = BozoBotDB.getCollection("UserMoments");
    public static final MongoCollection<Document> Misc = BozoBotDB.getCollection("Misc");
    public static final MongoCollection<Document> LFGPostDB = BozoBotDB.getCollection("LFGPosts");
    public static final MongoCollection<Document> StarboardPostDB = BozoBotDB.getCollection("StarboardPosts");
    public static final MongoCollection<Document> PollDB = BozoBotDB.getCollection("Polls");

    public static void main(String[] args)
    {

    }
}
