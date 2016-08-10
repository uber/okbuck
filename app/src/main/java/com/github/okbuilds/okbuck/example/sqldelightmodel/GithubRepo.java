package com.github.okbuilds.okbuck.example.sqldelightmodel;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

/**
 * Created by Piasy{github.com/Piasy} on 6/30/16.
 */
@AutoValue
public abstract class GithubRepo implements GithubRepoModel {
    public static TypeAdapter<GithubRepo> typeAdapter(final Gson gson) {
        return new AutoValue_GithubRepo.GsonTypeAdapter(gson);
    }

    public static GithubRepo create(long id, String name, String desc) {
        return new AutoValue_GithubRepo(id, name, desc);
    }
}
