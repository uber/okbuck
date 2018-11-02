package com.uber.okbuck.example.sqldelightmodel;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class GithubUser implements GithubUsers {
  public static TypeAdapter<GithubUser> typeAdapter(final Gson gson) {
    return new AutoValue_GithubUser.GsonTypeAdapter(gson);
  }

  public static GithubUser create(long id, String login) {
    return new AutoValue_GithubUser(id, login);
  }
}
