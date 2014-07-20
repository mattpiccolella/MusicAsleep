package com.hackfest.greylock.musicasleep;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.InputStream;

/**
 * A track to encapsulate a Spotify track, including artist, name, and a Bitmap for the image.
 */
public class SpotifyTrack {
    private String trackName;
    private String artistName;
    private String albumURL;
    private String trackID;

    public SpotifyTrack(String track, String artist, String albumURL, String trackID) {
        this.trackName = track;
        this.artistName = artist;
        this.albumURL = albumURL;
        this.trackID = trackID;
    }
    public String getTrackName() {
        return this.trackName;
    }
    public String getArtistName() {
        return this.artistName;
    }
    public String getAlbumURL() {
        return this.albumURL;
    }
    public String getTrackId() {
        return this.trackID;
    }
    public String toString() {
        return this.trackName + " " + this.artistName;
    }
}
