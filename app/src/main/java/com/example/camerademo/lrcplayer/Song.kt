package com.example.camerademo.lrcplayer

data class Song(val artist: String, val name: String,
                val mp3: String, val lyric: String) {
    companion object {
        @JvmStatic val defaultSong = Song(
            "Soda Stereo",
        "Persiana Americana",
        "https://ks-videos-prod.s3.amazonaws.com/277/277.mp3?AWSAccessKeyId=AKIAJKNSOOAN4I3YXIMA&Signature=IJh6mv6yyOW%2BhVDlqUgUfrEWWh4%3D&Expires=2091336774",
         "https://ks-videos-prod.s3.amazonaws.com/277/277.lrc?AWSAccessKeyId=AKIAJKNSOOAN4I3YXIMA&Signature=qsWFZqSHGfJftGy3MH7yb5DgGtk%3D&Expires=2091336744"
        )
    }
}