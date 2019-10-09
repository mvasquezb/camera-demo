package com.example.camerademo

data class Song(val artist: String, val name: String,
                val mp3: String, val lyric: String) {
    companion object {
        @JvmStatic val defaultSong = Song(
            "Soda Stereo",
            "Persiana Americana",
            "/storage/emulated/0/Android/data/com.example.camerademo/files/Download/Persiana Americana Letra.mp3",
            "https://ks-videos-prod.s3.amazonaws.com/277/277.lrc?AWSAccessKeyId=AKIAJKNSOOAN4I3YXIMA&Signature=qsWFZqSHGfJftGy3MH7yb5DgGtk%3D&Expires=2091336744"
        )
    }
}
