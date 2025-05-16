package cordova.plugin.videocall.RetrofitAPi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitAPi {

    //public  static  String accestokenURL;
    public static Retrofit getRetrofitService(){
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        OkHttpClient client = new OkHttpClient.Builder().build();
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        return new Retrofit.Builder()
//                .baseUrl("https://c9dev2.cloud9download.com:8080/")
                .baseUrl("https://uat.cloud9download.com:8080/")
//                .baseUrl("https://c9demo.cloud9download.com:8080/")
//                .baseUrl("https://alaska.cloud9download.com:8080/")
//                .baseUrl("https://mhid.cloud9download.com:8080/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

}

