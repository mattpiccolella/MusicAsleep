// Music Asleep Application for Greylock Hackfest 2014
// Matt Piccolella, Zach Lawrence, Josh Drubin, Char Kwon
package com.hackfest.greylock.musicasleep;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.AudioManager;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.spotify.sdk.android.Spotify;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.authentication.SpotifyAuthentication;
import com.spotify.sdk.android.playback.ConnectionStateCallback;
import com.spotify.sdk.android.playback.Player;
import com.spotify.sdk.android.playback.PlayerNotificationCallback;
import com.hackfest.greylock.musicasleep.OnPictureTaken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

public class MainActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback, OnPictureTaken {

    private static final String CLIENT_ID = "3acf3492794d499a87be2120198d616c";
    private static final String REDIRECT_URI = "music-asleep-login://callback";
    private static final String TRACK_PREFIX = "spotify:track:";
    private static final String SPOTIFY_TRACK_QUERY = "http://music-asleep.herokuapp.com/v1.0/select_song/";

    private Button nextSongButton;
    private ImageView albumArtwork;
    private TextView songName;
    private TextView artistName;
    private SeekBar seekBar;
    private CustomCamera mCustomCamera;
    private Button faceButton;

    private Player mPlayer;
    private SpotifyTrack currentTrack;
    private int currentSongLength;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCustomCamera = new CustomCamera(MainActivity.this);
        mCustomCamera.setPictureTakenListner(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        SpotifyAuthentication.openAuthWindow(CLIENT_ID, "token", REDIRECT_URI,
                new String[]{"user-read-private", "streaming"}, null, this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            AuthenticationResponse response = SpotifyAuthentication.parseOauthResponse(uri);
            Spotify spotify = new Spotify(response.getAccessToken());
            mPlayer = spotify.getPlayer(this, "Greylock Hackfest 2014", this, new Player.InitializationObserver() {
                @Override
                public void onInitialized() {
                    mPlayer.addConnectionStateCallback(MainActivity.this);
                    mPlayer.addPlayerNotificationCallback(MainActivity.this);
                    playRandomSongAndSetData(400);
                    final Handler nextSongHandler = new Handler();
                    nextSongHandler.postDelayed(new Runnable() {
                        int previousNumber = 0;
                        public void run() {
                            int currentTime = mPlayer.getPlayerState().positionInMs;
                            if (currentTime == previousNumber && (currentTime >= (currentSongLength * .8))) {
                                currentSongLength = Integer.MAX_VALUE;
                                playRandomSongAndSetData(getCurrentSleepScore());
                            }
                            previousNumber = mPlayer.getPlayerState().positionInMs;
                            nextSongHandler.postDelayed(this, 1000);
                        }
                    }, 1000);
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                }
            });
            nextSongButton = (Button) findViewById(R.id.next_song_button);
            songName = (TextView) findViewById(R.id.song_name);
            artistName = (TextView) findViewById(R.id.artist_name);
            albumArtwork = (ImageView) findViewById(R.id.album_artwork);
            nextSongButton.setEnabled(true);
            nextSongButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    playRandomSongAndSetData(getCurrentSleepScore());
                }
            });
            faceButton = (Button) findViewById(R.id.face_button);
            faceButton.setEnabled(true);
            faceButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mCustomCamera.startCameraFront();
                }
            });
            seekBar = (SeekBar) findViewById(R.id.seekBar);
            seekBar.setMax(100);
            seekBar.setProgress(40);
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void pictureTaken(Bitmap bitmap, File file) {
        seekBar.setProgress(seekBar.getProgress() + 10);
        playRandomSongAndSetData(getCurrentSleepScore());
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onNewCredentials(String s) {
        Log.d("MainActivity", "User credentials blob received");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void playRandomSongAndSetData(int score) {
        String trackRequest = SPOTIFY_TRACK_QUERY + "" + (score * 10);
        System.out.println(trackRequest);
        new RESTfulAPIService().execute(trackRequest);
    }

    public String getTrackUri(String trackId) {
        return TRACK_PREFIX + trackId;
    }

    public int getCurrentSleepScore() {
        return seekBar.getProgress();
    }

    private boolean eyesClosed(Bitmap myBitmap) {
        FaceDetector.Face[] myFace = new FaceDetector.Face[1];
        FaceDetector.Face face = myFace[0];
        PointF myMidPoint = new PointF();
        face.getMidPoint(myMidPoint);

        float myEyesDistance = face.eyesDistance();

        float whiteCountRight = 0;
        float totalCountRight = 0;
        for (int x = (int)(myMidPoint.x - myEyesDistance); x <= (myMidPoint.x - myEyesDistance + (myEyesDistance/1.25)); x++) {
            for (int y = (int)(myMidPoint.y - (myEyesDistance/3.25)); y <= (myMidPoint.y + (myEyesDistance/3.25)); y++) {
                int full_color = myBitmap.getPixel(x, y);
                if (Color.red(full_color) < 50 && Color.green(full_color) < 50 && Color.blue(full_color) < 50) {
                    whiteCountRight++;
                }
                totalCountRight++;
            }
        }
        float whitePercentRight = whiteCountRight / totalCountRight;
        System.out.println("z_whitePercentRight: " + whitePercentRight);

        float whiteCountLeft = 0;
        float totalCountLeft = 0;
        for (int x = (int)(myMidPoint.x + myEyesDistance - (myEyesDistance/1.25)); x <= (myMidPoint.x + myEyesDistance); x++) {
            for (int y = (int)(myMidPoint.y - (myEyesDistance/3.25)); y <= (myMidPoint.y + (myEyesDistance/3.25)); y++) {
                int full_color = myBitmap.getPixel(x, y);
                if (Color.red(full_color) < 50 && Color.green(full_color) < 50 && Color.blue(full_color) < 50) {
                    whiteCountLeft++;
                }
                totalCountLeft++;
            }
        }
        float whitePercentLeft = whiteCountLeft / totalCountLeft;
        System.out.println("z_whitePercentLeft: " + whitePercentLeft);

        // JL = .10327869, .3760116
        // Face = .03178808, .09288504
        // Closed = 0, 0.053276177
        // Closed2 = .008506032, .017690392
        // open = .12511854, .09014936
        // black_open = .038918283, .12792476
        // closed3 = 0, .01702215

        return (whitePercentLeft < 0.03 && whitePercentRight < 0.03);
    }

    private class RESTfulAPIService extends AsyncTask<String, Void, String> {
        protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException, IOException {
            InputStream in = entity.getContent();
            StringBuffer out = new StringBuffer();
            int byteCount = 1;
            while (byteCount > 0) {
                byte[] bytesRead = new byte[4096];
                byteCount =  in.read(bytesRead);
                if (byteCount > 0) {
                    out.append(new String(bytesRead, 0, byteCount));
                }
            }
            return out.toString();
        }
        @Override
        protected String doInBackground(String... params) {
            String urlParam = params[0];
            System.out.println("My URL: " + urlParam);
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(urlParam);
            String textResponse = null;
            try {
                HttpResponse response = httpClient.execute(httpGet, localContext);
                HttpEntity entity = response.getEntity();
                textResponse = getASCIIContentFromEntity(entity);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return textResponse;
        }
        @Override
        protected void onPostExecute(String results) {
            if (results != null) {
                try {
                    System.out.println(results);
                    JSONObject responseJson = new JSONObject(results);
                    if (responseJson.getString("status").equals("error")) {
                        return;
                    }
                    String trackName = responseJson.getString("track_name");
                    String currentArtistName = responseJson.getString("artist_name");
                    String albumURL = responseJson.getString("album_url");
                    String trackId = responseJson.getString("track_id");
                    songName.setText(trackName);
                    artistName.setText(currentArtistName);
                    new AlbumArtworkDownloader().execute(albumURL);
                    mPlayer.play(getTrackUri(trackId));
                    currentTrack = new SpotifyTrack(trackName, currentArtistName, albumURL, trackId);
                    currentSongLength = Integer.parseInt(responseJson.getString("duration"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private class AlbumArtworkDownloader extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            String imageUrl = urls[0];
            Bitmap albumCover = null;
            try {
                InputStream in = new java.net.URL(imageUrl).openStream();
                albumCover = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return albumCover;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            albumArtwork.setImageBitmap(result);
        }
    }
}