package backend.tidalwave.dolbydlteam

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

fun getTrackInfo(clientID: String, bearerToken: String, trackID: Int): String{
    val baseAPIURL = "https://api.tidal.com/v1"
    val params = "?playbackmode=STREAM&assetpresentation=FULL&audioquality=HI_RES&prefetch=false&countryCode=US&limit=9999"
    //val cookieJar = JavaNetCookieJar(CookieManager())
    val songInfoURL = URL("$baseAPIURL/tracks/$trackID/playbackinfopostpaywall$params")
    val getSongInfo = songInfoURL.openConnection() as HttpURLConnection
    getSongInfo.setRequestProperty("X-Tidal-Token", clientID)
    getSongInfo.setRequestProperty("Accept", "*/*")
    getSongInfo.setRequestProperty("Connection", "Keep-Alive")
    getSongInfo.setRequestProperty("Host", "api.tidal.com")
    getSongInfo.setRequestProperty("Accept-Encoding", "gzip")
    getSongInfo.setRequestProperty("Authorization", "Bearer $bearerToken")
    getSongInfo.setRequestProperty("User-Agent", "TIDAL_ANDROID/1000 okhttp/3.13.1")

    val songInfo = JSONObject(getSongInfo.inputStream.bufferedReader().readText())
    //print(songInfo)
    val manifest = String(Base64.getDecoder().decode(songInfo.getString("manifest")))
    //println(manifest)
    val songURL = JSONObject(manifest).getJSONArray("urls")
    return songURL[0].toString()


    /*val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    var params = "?playbackmode=STREAM&assetpresentation=FULL&audioquality=HI_RES&prefetch=false&countryCode=US&limit=9999"
    val trackInfoRequest = Request.Builder()
            .url("$baseAPIURL/tracks/$trackID/playbackinfopostpaywall$params")
            .addHeader("User-Agent", "TIDAL_ANDROID/1000 okhttp/3.13.1")
            .addHeader("Accept-Encoding", "gzip")*/
            //.addHeader("Accept", "*/*")
            /*.addHeader("Connection", "Keep-Alive")
            .addHeader("Host", "api.tidal.com")
            .addHeader("X-Tidal-Token", clientID)
            .addHeader("Authorization", "Bearer $bearerToken")
            .build()
    val trackInfoResponse = client.newCall(trackInfoRequest).execute()
    trackInfoResponse.use {response ->
        println(response.body!!.string())
    }*/
    //println(trackInfoRequest.body!!)

    //println(trackInfoJSON)
    /*
    if (!trackInfoRequest.isSuccessful) throw IOException("Unexpected code $trackInfoRequest")
    val trackInfo = JSONObject(trackInfoRequest.body!!.string())
    val audioQuality = trackInfo.getString("audioQuality")
    println("Audio Quality: $audioQuality")*/
}

fun getIDbySearch(clientID: String, bearerToken: String, query: String, searchType: String): JSONObject {
    val baseAPIURL = "https://api.tidal.com/v1"
    val params = "?query=" + query.replace(" ", "%20") + "&offset=0&limit=10&includeContributors=true&countryCode=US"
    //val cookieJar = JavaNetCookieJar(CookieManager())
    val searchURL = URL("$baseAPIURL/search$params")
    val searchRequest = searchURL.openConnection() as HttpURLConnection
    searchRequest.setRequestProperty("X-Tidal-Token", clientID)
    searchRequest.setRequestProperty("Accept", "*/*")
    searchRequest.setRequestProperty("Connection", "Keep-Alive")
    searchRequest.setRequestProperty("Host", "api.tidal.com")
    searchRequest.setRequestProperty("Accept-Encoding", "gzip")
    searchRequest.setRequestProperty("Authorization", "Bearer $bearerToken")
    searchRequest.setRequestProperty("User-Agent", "TIDAL_ANDROID/1000 okhttp/3.13.1")
    val searchBody = searchRequest.inputStream.bufferedReader().readText()
    return JSONObject(searchBody).getJSONObject(searchType)
}
