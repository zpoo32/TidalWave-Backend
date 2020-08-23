package backend.tidalwave.dolbydlteam

import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.CookieManager
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random


fun getBearerToken(clientID: String, email: String, password: String, debugOutput: Boolean): String {
    val baseLoginURL = "https://login.tidal.com"
    val baseAuthURL = "https://auth.tidal.com/v1"
    val randomSeed = Random.nextBytes(32)
    if (debugOutput) {println("Random seed in hex: " + randomSeed.fold("", { str, it -> str + "%02x".format(it) }))}
    //val randomSeed = ("815292f9c9e73fe25c88bfaf6072aee8db8b853f27f96ca9cd2483e464a723a6").chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    val codeVerifier = Base64.getEncoder().encodeToString(randomSeed).replace("=", "").replace("/", "_").replace("+", "-")
    if (debugOutput) {println("Code verifier: $codeVerifier")}
    val codeChallenge: String = Base64.getEncoder().encodeToString(hash(codeVerifier)).replace("=", "").replace("/", "_").replace("+", "-")
    if (debugOutput) {println("SHA256 hash of code verifier: " + hash(codeVerifier).fold("", { str, it -> str + "%02x".format(it) }))}
    if (debugOutput) {println("Code challenge: $codeChallenge")}
    val clientUniqueKey = toHexString(Random.nextBytes(16))
    val params = "?response_type=code&redirect_uri=https%3A%2F%2Ftidal.com%2Fandroid%2Flogin%2Fauth&lang=en_US&appMode=android&client_id=$clientID&client_unique_key=$clientUniqueKey&code_challenge=$codeChallenge&code_challenge_method=S256"
    //if (debugOutput) {println("Final parameters: $params")}

    val cookieJar = JavaNetCookieJar(CookieManager())
    /*
    val cookieJar: CookieJar = object : CookieJar {
        private val cookieStore =
            HashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val cookies = cookieStore[url.host]
            return cookies ?: ArrayList()
        }
    } */

    val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

    // Get CSRF token
    val request = Request.Builder()
            .url("$baseLoginURL/authorize$params")
            .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        /*for ((name, value) in response.headers) {
            println("$name: $value")
        }*/
    }

    //println(cookieJar.loadForRequest(request.url)[2])
    // WARNING: CSRF token retrieval method is hardcoded, does not dynamically select the cookie named "token"
    // Check if email exists
    var token = client.cookieJar.loadForRequest(request.url)[2].toString().split("=")[1].split(";")[0]
    val emailCheckJson = "{\"_csrf\": \"$token\", \"email\": \"$email\", \"recaptchaResponse\": \"\"}"
    val jsonBody: RequestBody = emailCheckJson.toRequestBody("application/json".toMediaTypeOrNull())
    val jsonRequest: Request = Request.Builder()
        .url("$baseLoginURL/email$params")
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "*///*")
        .addHeader("Accept-Encoding", "gzip, deflate")
        .addHeader("User-Agent", "TIDAL_ANDROID/1000 okhttp/3.13.1")
        .post(jsonBody)
        .build()
    client.newCall(jsonRequest).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val respBody = JSONObject(response.body!!.string())
        //println(respBody)
        val isInvalidAccount = respBody.getBoolean("newUser")
        if (isInvalidAccount) throw IOException("An account with email $email does not exist!")

    }

    // Login
    // Hardcoded parse again
    token = client.cookieJar.loadForRequest(request.url)[2].toString().split("=")[1].split(";")[0]
    val loginJson = "{\"_csrf\": \"$token\", \"email\": \"$email\", \"password\": \"$password\"}"
    val body: RequestBody = loginJson.toRequestBody("application/json".toMediaTypeOrNull())
    val loginRequest: Request = Request.Builder()
        .url("$baseLoginURL/email/user/existing$params")
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "*/*")
        .addHeader("Accept-Encoding", "gzip, deflate")
        //.addHeader("Cookie", fullCookieHeader)
        .addHeader("User-Agent", "TIDAL_ANDROID/1000 okhttp/3.13.1")
        .post(body)
        .build()
    client.newCall(loginRequest).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        //println(response.body!!.string())
    }

    // Get access token
    val successfulLoginRequest = Request.Builder()
            .url("$baseLoginURL/success?lang=en")
            .build()
    val loginResponse = client.newCall(successfulLoginRequest).execute()
    loginResponse.use {}

    // Trade access token for authorization token
    // Hardcoded parsing
    val accessCode = loginResponse.header("Location").toString().split("=")[1].split("&")[0]
    if (debugOutput) {println("Access code: $accessCode")}
    val getOAuthBody = ("code=$accessCode&client_id=$clientID&grant_type=authorization_code&redirect_uri=https%3A%2F%2Ftidal.com%2Fandroid%2Flogin%2Fauth&scope=r_usr+w_usr+w_sub&code_verifier=$codeVerifier&client_unique_key=$clientUniqueKey").toRequestBody()
    val getOAuthRequest: Request = Request.Builder()
            .url("$baseAuthURL/oauth2/token")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "*/*")
            .addHeader("Accept-Encoding", "gzip, deflate")
            .addHeader("User-Agent", "TIDAL_ANDROID/1000 okhttp/3.13.1")
            .post(getOAuthBody)
            .build()
    var accessToken = ""
    client.newCall(getOAuthRequest).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        // Hardcoded parse AGAIN
        accessToken = response.body!!.string().split("\"")[3]
    }
    if (debugOutput) {println("Bearer token: $accessToken")}
    return accessToken
}

// Gets SHA-256 has of any bytearray that is input
fun hash(toHash: String): ByteArray {
    val bytes = toHash.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    //val digest = md.digest(bytes)
    //val folded = digest.fold("", { str, it -> str + "%02x".format(it) }) # Bytearray to hex
    //return folded.chunked(2).map { it.toInt(16).toByte() }.toByteArray() # Hex to bytearray
    return md.digest(bytes)
}

// Converts a bytearray into hexadecimal
fun toHexString(toHex: ByteArray): String {
    return toHex.joinToString("") {
        java.lang.String.format("%02x", it)
    }
}