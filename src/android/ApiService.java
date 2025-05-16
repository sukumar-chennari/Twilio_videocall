package cordova.plugin.videocall.ApiService;

import cordova.plugin.videocall.MyResponse.MyResponse;
import io.reactivex.Single;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import src.cordova.plugin.videocall.CompleteRoomResponse;
import src.cordova.plugin.videocall.Response;

public interface ApiService {

    @FormUrlEncoded
    @POST("/api/Users/getToken")
    Single<MyResponse> getData(@Field("roomName") String roomName,
                               @Field("identity") String identity);

    @FormUrlEncoded
    @POST("/api/twilio-video/completeRoom")
    Single<CompleteRoomResponse> completeRoom(@Field("sid") String sid);

    @GET("/api/AvailabilityLogs/by-room-name")
    Single<Response> getAllRoomParticipants(@Query("roomName") String roomName);

    @FormUrlEncoded
    @POST("/api/Users/sendnotification")
    Single<Object> sendNotification(@Field("roomName") String roomName,
                                    @Field("roomSid") String roomSid,
                                    @Field("identity") String identity,
                                    @Field("user_type") String role,
                                    @Field("senderId") String senderId,
                                    @Field("receivedId") String receivedId,
                                    @Field("called_in") String calledIn,
                                    @Header("Authorization") String accessToken);
}
