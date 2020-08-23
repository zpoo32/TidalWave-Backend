package backend.tidalwave.dolbydlteam

import org.json.JSONObject

fun main() {
    val clientID = "dN2N95wCyEBTllu4"
    val email = ""
    val password = ""
    val debugOutput = true
    val bearer = getBearerToken(clientID, email, password, debugOutput)
    val searchBody = getIDbySearch(clientID, bearer, "Alan Walker Darkside", "tracks")
    var numberOfSongs = 10
    if (searchBody.getInt("totalNumberOfItems") < 10) {
        numberOfSongs = searchBody.getInt("totalNumberOfItems")
    }
    println("Song results: ")
    for (i in 0 until numberOfSongs) {
        val songDetails = JSONObject(searchBody.getJSONArray("items")[i].toString())
        val songName = songDetails.getString("title")
        val songArtist = JSONObject(songDetails.getJSONArray("artists")[0].toString()).getString("name")
        println((i+1).toString() + ". $songName by $songArtist")
    }
    val songSelectionNumber: Int = 1-1
    val songID = JSONObject(searchBody.getJSONArray("items")[songSelectionNumber].toString()).getInt("id")

    //val songID = "131069353"
    val songURL = getTrackInfo(clientID, bearer, songID)
    println("Track URL of selection " + (songSelectionNumber+1) + ": $songURL")
}


