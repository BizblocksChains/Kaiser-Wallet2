package io.kaiser.kaiserwallet2.Network;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.POST;

public interface NetApiService {

    @POST("/ready")
    Call<JsonObject> postReady();

    

}
