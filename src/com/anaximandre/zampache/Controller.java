/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Daniel Schruhl, 2015 Anthony Gasperin
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package com.anaximandre.zampache;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;

import com.anaximandre.ampache.Album;
import com.anaximandre.ampache.Artist;
import com.anaximandre.ampache.Playlist;
import com.anaximandre.ampache.Song;
import com.anaximandre.zampache.views.SelectedAlbumsView;
import com.anaximandre.zampache.views.SelectedArtistsView;
import com.anaximandre.zampache.views.SelectedSongsView;
import com.anaximandre.zampache.views.AlbumsView.AsyncTaskCallback;
import com.anaximandre.zampache.views.ArtistsView.AsyncAlbumTask;

/**
 * @author Daniel Schruhl, 2015 Gasperin Anthony
 * 
 */

public class Controller {

	private static Controller controller = null;
	private ServerConnector server;
	private String[] fragments;
	private String[] fragmentsNames;
	private ArrayList<Playlist> playlists;
	private ArrayList<Song> songs;
	private ArrayList<Artist> artists;
	private ArrayList<Album> albums;
	private ArrayList<Song> playNow;
	private int playNowPosition;
	private Song playingNow;
	private int progress = 0;
	private ArrayList<Song> selectedSongs;
	private ArrayList<Artist> selectedArtists;
	private ArrayList<Album> selectedAlbums;
	private ArrayList<Playlist> selectedPlaylists;
	private final String ARTIST_VIEW_FRAGMENT="artist_view_fragment";
	/**
	 * 
	 */
	public Controller(String[] navigationNames) {
		if (navigationNames == null) {
			navigationNames = new String[6];
			navigationNames[0] = "Now Playing";
			navigationNames[1] = "Songs";
			navigationNames[2] = "Artists";
			navigationNames[3] = "Albums";
			navigationNames[4] = "Playlists";
			navigationNames[5] = "Settings";
		}
		init(navigationNames);
	}

	/**
	 * Method to initialize the controller.
	 */
	private void init(String[] navigationNames) {
		fragments = new String[6];
		fragments[0] = "com.anaximandre.zampache.views.CurrentPlaylistView";
		fragments[1] = "com.anaximandre.zampache.views.SongsView";
		fragments[2] = "com.anaximandre.zampache.views.ArtistsView";
		fragments[3] = "com.anaximandre.zampache.views.AlbumsView";
		fragments[4] = "com.anaximandre.zampache.views.PlaylistsView";
		fragments[5] = "com.anaximandre.zampache.views.SettingsView";

		fragmentsNames = new String[6];
		fragmentsNames[0] = navigationNames[0];
		fragmentsNames[1] = navigationNames[1];
		fragmentsNames[2] = navigationNames[2];
		fragmentsNames[3] = navigationNames[3];
		fragmentsNames[4] = navigationNames[4];
		fragmentsNames[5] = navigationNames[5];

		this.playlists = new ArrayList<Playlist>();
		this.songs = new ArrayList<Song>();
		this.playNow = new ArrayList<Song>();
		this.selectedSongs = new ArrayList<Song>();
		this.selectedAlbums = new ArrayList<Album>();
		this.selectedArtists = new ArrayList<Artist>();
		this.selectedPlaylists = new ArrayList<Playlist>();
		this.artists = new ArrayList<Artist>();
		this.albums = new ArrayList<Album>();
		this.server = new ServerConnector("", "", "");
	}

	public void saveSettings(String password, String user, String host) {
		Log.d("passwort-raw:", password);
		Log.d("passwort-raw:", generateShaHash(password));
		this.server.setPassword(password);
		this.server.setUser(user);
		this.server.setHost(host);
	}

	/**
	 * @return the fragmentsNames
	 */
	public String[] getFragmentsNames() {
		return fragmentsNames;
	}

	/**
	 * @param fragmentsNames the fragmentsNames to set
	 */
	public void setFragmentsNames(String[] fragmentsNames) {
		this.fragmentsNames = fragmentsNames;
	}

	/**
	 * @return the fragments
	 */
	public String[] getFragments() {
		return fragments;
	}

	/**
	 * @param fragments the fragments to set
	 */
	public void setFragments(String[] fragments) {
		this.fragments = fragments;
	}

	/**
	 * Allows to get the only Instance of the android controller singleton.
	 * 
	 * @return the instance of this controller class
	 */
	public static Controller getInstance() {
		if (controller == null) {
			controller = new Controller(null);
		}
		return controller;
	}

	/**
	 * Allows to get the only Instance of the android controller singleton. Also enables localization for the menu items
	 * 
	 * @param String[] Names of the menu items
	 * @return the instance of this controller class
	 */
	public static Controller getInstance(String[] navigationNames) {
		if (controller == null) {
			controller = new Controller(navigationNames);
		}
		return controller;
	}

	/**
	 * 
	 * @param s
	 * @return
	 */
	@SuppressLint("DefaultLocale")
	public String generateShaHash(String s) {
		String hash = "";
		hash = bin2hex(getHash(s)).toLowerCase();
		return hash;
	}

	/**
	 * 
	 * @param password
	 * @return
	 */
	public byte[] getHash(String password) {
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		digest.reset();
		return digest.digest(password.getBytes());
	}

	/**
	 * 
	 * @param data
	 * @return
	 */
	static String bin2hex(byte[] data) {
		return String.format("%0" + (data.length * 2) + "X", new BigInteger(1, data));
	}

	public boolean saveServer(Context context) {
		try {
			FileOutputStream fos = context.openFileOutput("config", Context.MODE_PRIVATE);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.server);
			// Log.d("ser", server.getAmpacheConnection().getAuth());
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
			Log.d("bug", "Speichern nicht erfolgreich");
			return false;
		}
		return true;
	}

	/**
	 * Returns if online connectivity is given or not.
	 * 
	 * @param context ApplicationContext
	 * @return {@code true} if connected to Internet, {@code false} otherwise
	 */
	public boolean isOnline(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}

	public void loadCachedFiles() {
		this.setSongs(this.server.getCachedData().getSongs());
		this.setAlbums(this.server.getCachedData().getAlbums());
		this.setArtists(this.server.getCachedData().getArtists());
		this.setPlaylists(this.server.getCachedData().getPlaylists());
	}

	public ServerConnector getServerConfig(Context context) {
		try {
			FileInputStream fis = context.openFileInput("config");
			ObjectInputStream is = new ObjectInputStream(fis);
			Object readObject = is.readObject();
			is.close();

			if (readObject != null && readObject instanceof ServerConnector) {
				setServer((ServerConnector) readObject);
				// Log.d("ser", ((ServerConnector) readObject).getAmpacheConnection().getAuth());
				return (ServerConnector) readObject;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public ArrayList<Song> findSongs(Artist artist) {
		ArrayList<Song> result = new ArrayList<Song>();
		for (Song s : songs) {
			if (s.getArtist().equals(artist.getName())) {
				result.add(s);
				this.progress++;
			}
		}
		return result;
	}

	/* Need to send a request albums_songs&filter=album.getId() 
	 * 	In an asynchronous task */
	public ArrayList<Song> findSongs(Album album,AsyncTaskCallback t) {
		Log.d("findSongs","album.getId()="+album.getId());
		ArrayList<Song> result = new ArrayList<Song>();
		new DownloadSongsXml(t).execute(album);
		/*for (Song s : songs) {
			if (s.getAlbum().equals(album.getName())) {
				result.add(s);
			}
		}*/
		
		return result;
	}
	
	public ArrayList<Song> findSongs(Album album) {
		Log.d("findSongs","album.getId()="+album.getId());
		ArrayList<Song> result = new ArrayList<Song>();
		Song fake_song = new Song();
		fake_song.setTitle("FAKE Chanson ");
		result.add(fake_song);
		//new DownloadSongsXml().execute(album);
		/*for (Song s : songs) {
			if (s.getAlbum().equals(album.getName())) {
				result.add(s);
			}
		}*/
		
		return result;
	}
	public ArrayList<Album> findAlbums(Artist artist,AsyncAlbumTask asa) {
		ArrayList<Album> result = new ArrayList<Album>();
		/*for (Album a : albums) {
			if (a.getArtist().equals(artist.getName())) {
				result.add(a);
			}
		}*/
		new DownloadAlbumsFromArtistXml(asa).execute(artist);
		return result;
	}

	/**
	 * @return the server
	 */
	public ServerConnector getServer() {
		return server;
	}

	/**
	 * @param server the server to set
	 */
	public void setServer(ServerConnector server) {
		this.server = server;
	}

	public void parseSongs(String urlString) {
		Log.d("songs", urlString);
		Log.d("songs anzahl", String.valueOf(server.getAmpacheConnection().getSongs()));
		URL url;
		try {
			url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.connect();
			XmlPullParserFactory pullParserFactory;
			try {
				pullParserFactory = XmlPullParserFactory.newInstance();
				XmlPullParser parser = pullParserFactory.newPullParser();
				InputStream in_s = con.getInputStream();
				parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
				parser.setInput(in_s, null);
				parseSongsXML(parser);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			Log.d("error", "keine Verbindung möglich");
			e.printStackTrace();
		}
	}

	public void parsePlaylistSongs(String urlString, ArrayList<Song> playlist) {
		Log.d("playlist Songs", urlString);
		URL url;
		try {
			url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.connect();
			XmlPullParserFactory pullParserFactory;
			try {
				pullParserFactory = XmlPullParserFactory.newInstance();
				XmlPullParser parser = pullParserFactory.newPullParser();
				InputStream in_s = con.getInputStream();
				parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
				parser.setInput(in_s, null);
				parsePlaylistSongsXML(parser, playlist);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			Log.d("error", "keine Verbindung möglich");
			e.printStackTrace();
		}
	}

	private void parsePlaylistSongsXML(XmlPullParser parser, ArrayList<Song> songlist) throws XmlPullParserException, IOException {
		int eventType = parser.getEventType();
		Song currentSong = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			String name = null;
			switch (eventType) {
			case XmlPullParser.START_TAG:
				name = parser.getName();
				Log.d("playlist songs titles:", name);
				if (name.equals("song")) {
					currentSong = new Song();
					currentSong.setId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
				} else if (currentSong != null) {
					if (name.equals("title")) {
						currentSong.setTitle(parser.nextText());
					} else if (name.equals("artist")) {
						currentSong.setArtist(parser.nextText());
						// currentSong.setArtistId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
					} else if (name.equals("album")) {
						currentSong.setAlbum(parser.nextText());
						// currentSong.setAlbumId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
					} else if (name.equals("track")) {
						currentSong.setTrackNumber(Integer.parseInt(parser.nextText()));
					} else if (name.equals("time")) {
						currentSong.setTime(Integer.parseInt(parser.nextText()));
					} else if (name.equals("year")) {
						currentSong.setYear(Integer.parseInt(parser.nextText()));
					} else if (name.equals("bitrate")) {
						currentSong.setBitrate(Integer.parseInt(parser.nextText()));
					} else if (name.equals("mode")) {
						currentSong.setMode(parser.nextText());
					} else if (name.equals("mime")) {
						currentSong.setMime(parser.nextText());
					} else if (name.equals("url")) {
						currentSong.setUrl(parser.nextText());
					} else if (name.equals("size")) {
						currentSong.setSize(Integer.parseInt(parser.nextText()));
					} else if (name.equals("art")) {
						currentSong.setArt(parser.nextText());
					}
				}
				break;
			case XmlPullParser.END_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase("song") && currentSong != null) {
					songlist.add(currentSong);
				}
			}
			eventType = parser.next();
		}
		Log.d("songs:", songlist.toString());
	}

	public void parsePlaylists(String urlString) {
		Log.d("playlists", urlString);
		Log.d("playlists anzahl", String.valueOf(server.getAmpacheConnection().getPlaylists()));
		URL url;
		try {
			url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.connect();
			XmlPullParserFactory pullParserFactory;
			try {
				pullParserFactory = XmlPullParserFactory.newInstance();
				XmlPullParser parser = pullParserFactory.newPullParser();
				InputStream in_s = con.getInputStream();
				parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
				parser.setInput(in_s, null);
				parsePlaylistsXML(parser);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			Log.d("error", "keine Verbindung möglich");
			e.printStackTrace();
		}
	}

	private void parsePlaylistsXML(XmlPullParser parser) throws XmlPullParserException, IOException {
		this.playlists = new ArrayList<Playlist>();
		int eventType = parser.getEventType();
		Playlist currentPlaylist = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			String name = null;
			switch (eventType) {
			case XmlPullParser.START_TAG:
				name = parser.getName();
				if (name.equals("playlist")) {
					currentPlaylist = new Playlist();
					currentPlaylist.setId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
				} else if (currentPlaylist != null) {
					if (name.equals("name")) {
						currentPlaylist.setName(parser.nextText());
					} else if (name.equals("owner")) {
						currentPlaylist.setOwner(parser.nextText());
					} else if (name.equals("items")) {
						currentPlaylist.setItems(parser.nextText());
					} else if (name.equals("type")) {
						currentPlaylist.setType(parser.nextText());
					}
				}
				break;
			case XmlPullParser.END_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase("playlist") && currentPlaylist != null) {
					this.playlists.add(currentPlaylist);
					this.progress++;
				}
			}
			eventType = parser.next();
		}
		Log.d("playlists:", this.playlists.toString());
	}

	private void parseSongsXML(XmlPullParser parser) throws XmlPullParserException, IOException {
		this.songs = new ArrayList<Song>();
		int eventType = parser.getEventType();
		Song currentSong = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			String name = null;
			switch (eventType) {
			case XmlPullParser.START_TAG:
				name = parser.getName();
				if (name.equals("song")) {
					currentSong = new Song();
					currentSong.setId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
				} else if (currentSong != null) {
					if (name.equals("title")) {
						currentSong.setTitle(parser.nextText());
					} else if (name.equals("artist")) {
						currentSong.setArtist(parser.nextText());
						// currentSong.setArtistId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
					} else if (name.equals("album")) {
						currentSong.setAlbum(parser.nextText());
						// currentSong.setAlbumId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
					} else if (name.equals("track")) {
						currentSong.setTrackNumber(Integer.parseInt(parser.nextText()));
					} else if (name.equals("time")) {
						currentSong.setTime(Integer.parseInt(parser.nextText()));
					} else if (name.equals("year")) {
						currentSong.setYear(Integer.parseInt(parser.nextText()));
					} else if (name.equals("bitrate")) {
						currentSong.setBitrate(Integer.parseInt(parser.nextText()));
					} else if (name.equals("mode")) {
						currentSong.setMode(parser.nextText());
					} else if (name.equals("mime")) {
						currentSong.setMime(parser.nextText());
					} else if (name.equals("url")) {
						currentSong.setUrl(parser.nextText());
					} else if (name.equals("size")) {
						currentSong.setSize(Integer.parseInt(parser.nextText()));
					} else if (name.equals("art")) {
						currentSong.setArt(parser.nextText());
					}
				}
				break;
			case XmlPullParser.END_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase("song") && currentSong != null) {
					this.songs.add(currentSong);
					this.progress++;
				}
			}
			eventType = parser.next();
		}
		Log.d("songs:", this.songs.toString());
	}

	public void parseAlbums(String urlString) {
		Log.d("albums", urlString);
		Log.d("albums anzahl", String.valueOf(server.getAmpacheConnection().getAlbums()));
		URL url;
		try {
			url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.connect();
			XmlPullParserFactory pullParserFactory;
			try {
				pullParserFactory = XmlPullParserFactory.newInstance();
				XmlPullParser parser = pullParserFactory.newPullParser();
				InputStream in_s = con.getInputStream();
				parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
				parser.setInput(in_s, null);
				parseAlbumsXML(parser);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			Log.d("error", "keine Verbindung möglich");
			e.printStackTrace();
		}
	}

	private void parseAlbumsXML(XmlPullParser parser) throws XmlPullParserException, IOException {
		this.albums = new ArrayList<Album>();
		int eventType = parser.getEventType();
		Album currentAlbum = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			String name = null;
			switch (eventType) {
			case XmlPullParser.START_TAG:
				name = parser.getName();
				if (name.equals("album")) {
					currentAlbum = new Album();
					currentAlbum.setId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
				} else if (currentAlbum != null) {
					if (name.equals("name")) {
						currentAlbum.setName(parser.nextText());
					} else if (name.equals("artist")) {
						// currentAlbum.setArtistId(Integer.parseInt(parser.getAttributeValue(null, "id")));
						currentAlbum.setArtist(parser.nextText());
					} else if (name.equals("tracks")) {
						currentAlbum.setTracks(Integer.parseInt(parser.nextText()));
					} else if (name.equals("art")) {
						currentAlbum.setArt(parser.nextText());
					}
				}
				break;
			case XmlPullParser.END_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase("album") && currentAlbum != null) {
					this.albums.add(currentAlbum);
					this.progress++;
				}
			}
			eventType = parser.next();
		}
		Log.d("albums:", this.albums.toString());
	}

	public void parseArtists(String urlString) {
		Log.d("artists", urlString);
		Log.d("artists anzahl", String.valueOf(server.getAmpacheConnection().getArtists()));
		URL url;
		try {
			url = new URL(urlString);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.connect();
			XmlPullParserFactory pullParserFactory;
			try {
				pullParserFactory = XmlPullParserFactory.newInstance();
				XmlPullParser parser = pullParserFactory.newPullParser();
				InputStream in_s = con.getInputStream();
				parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                                parser.setFeature(Xml.FEATURE_RELAXED, true);
				parser.setInput(in_s, null);
//				parseArtistsXML(parser);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			Log.d("error", "keine Verbindung möglich");
			e.printStackTrace();
		}
	}

	private void parseArtistsXML(XmlPullParser parser) throws XmlPullParserException, IOException {
		this.artists = new ArrayList<Artist>();
		int eventType = parser.getEventType();
		Artist currentArtist = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			String name = null;
			switch (eventType) {
			case XmlPullParser.START_TAG:
				name = parser.getName();
				if (name.equals("artist")) {
					currentArtist = new Artist();
					currentArtist.setId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
					
					
				} else if (currentArtist != null) {
					if (name.equals("name")) {
						currentArtist.setName(parser.nextText());
					} else if (name.equals("albums")) {
						currentArtist.setAlbums(Integer.parseInt(parser.nextText()));
					} else if (name.equals("songs")) {
						currentArtist.setSongs(Integer.parseInt(parser.nextText()));
					}
				}
				break;
			case XmlPullParser.END_TAG:
				name = parser.getName();
				if (name.equalsIgnoreCase("artist") && currentArtist != null) {
					this.artists.add(currentArtist);
					this.progress++;
				}
			}
			eventType = parser.next();
		}
		Log.d("artists:", this.artists.toString());
	}

	/**
	 * @return the playlists
	 */
	public ArrayList<Playlist> getPlaylists() {
		return playlists;
	}

	/**
	 * @param playlists the playlists to set
	 */
	public void setPlaylists(ArrayList<Playlist> playlists) {
		this.playlists = playlists;
	}

	/**
	 * @return the songs
	 */
	public ArrayList<Song> getSongs() {
		return songs;
	}

	/**
	 * @param songs the songs to set
	 */
	public void setSongs(ArrayList<Song> songs) {
		this.songs = songs;
	}

	/**
	 * @return the artists
	 */
	public ArrayList<Artist> getArtists() {
		return artists;
	}

	/**
	 * @param artists the artists to set
	 */
	public void setArtists(ArrayList<Artist> artists) {
		this.artists = artists;
	}

	/**
	 * @return the playNow
	 */
	public ArrayList<Song> getPlayNow() {
		return playNow;
	}

	/**
	 * @param playNow the playNow to set
	 */
	public void setPlayNow(ArrayList<Song> playNow) {
		this.playNow = playNow;
	}

	/**
	 * @return the playingNow
	 */
	public Song getPlayingNow() {
		return playingNow;
	}

	/**
	 * @param playingNow the playingNow to set
	 */
	public void setPlayingNow(Song playingNow) {
		this.playingNow = playingNow;
	}

	/**
	 * @return the albums
	 */
	public ArrayList<Album> getAlbums() {
		return albums;
	}

	/**
	 * @param albums the albums to set
	 */
	public void setAlbums(ArrayList<Album> albums) {
		this.albums = albums;
	}

	/**
	 * @return the progress
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * @param progress the progress to set
	 */
	public void setProgress(int progress) {
		this.progress = progress;
	}

	/**
	 * @return the playNowPosition
	 */
	public int getPlayNowPosition() {
		return playNowPosition;
	}

	/**
	 * @param playNowPosition the playNowPosition to set
	 */
	public void setPlayNowPosition(int playNowPosition) {
		this.playNowPosition = playNowPosition;
	}

	/**
	 * @return the selectedSongs
	 */
	public ArrayList<Song> getSelectedSongs() {
		return selectedSongs;
	}

	/**
	 * @param selectedSongs the selectedSongs to set
	 */
	public void setSelectedSongs(ArrayList<Song> selectedSongs) {
		this.selectedSongs = selectedSongs;
	}

	/**
	 * @return the selectedArtists
	 */
	public ArrayList<Artist> getSelectedArtists() {
		return selectedArtists;
	}

	/**
	 * @param selectedArtists the selectedArtists to set
	 */
	public void setSelectedArtists(ArrayList<Artist> selectedArtists) {
		this.selectedArtists = selectedArtists;
	}

	/**
	 * @return the selectedAlbums
	 */
	public ArrayList<Album> getSelectedAlbums() {
		return selectedAlbums;
	}

	/**
	 * @param selectedAlbums the selectedAlbums to set
	 */
	public void setSelectedAlbums(ArrayList<Album> selectedAlbums) {
		this.selectedAlbums = selectedAlbums;
	}

	/**
	 * @return the selectedPlaylists
	 */
	public ArrayList<Playlist> getSelectedPlaylists() {
		return selectedPlaylists;
	}

	/**
	 * @param selectedPlaylists the selectedPlaylists to set
	 */
	public void setSelectedPlaylists(ArrayList<Playlist> selectedPlaylists) {
		this.selectedPlaylists = selectedPlaylists;
	}
	
	/**
	 * An AsyncTask to download Songs of an Albums
	 *
	 * @author: Gasperin Anthony
	 */
	private class DownloadSongsXml extends AsyncTask<Album,Void,Void>{

		AsyncTaskCallback mAsyncTaskCallback;
		
		public DownloadSongsXml(AsyncTaskCallback tc){
			mAsyncTaskCallback=tc;
			
		}
		@Override
		protected Void doInBackground(Album... params) {
			// TODO Auto-generated method stub
			String urlString="";
			Log.d("SUPERASYNCTASK","album.getId()="+params[0].getId());
				Album a = params[0];
				urlString = controller.getServer().getHost() + "/server/xml.server.php?action=album_songs&auth="
						+ controller.getServer().getAuthKey()+"&filter="+a.getId();
			Log.d("doInBackground",urlString);
				URL url;
				try {
					url = new URL(urlString);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.connect();
					XmlPullParserFactory pullParserFactory;
					try {
						pullParserFactory = XmlPullParserFactory.newInstance();
						XmlPullParser parser = pullParserFactory.newPullParser();
						InputStream in_s = con.getInputStream();
						parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
						Log.d("SONGS",in_s.toString());
						parser.setInput(in_s, null);
						parseSongsXML(parser);
					} catch (XmlPullParserException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					Log.d("error", "keine Verbindung möglich");
					e.printStackTrace();
				}
			
			return null;
		}
	    @Override
	    protected void onPostExecute(Void result) {
	        mAsyncTaskCallback.onTaskDone(songs);
	    }
		
	}
	
	/**
	 * 
	 * @author Gasperin Anthony
	 *
	 */
	private class DownloadAlbumsFromArtistXml extends AsyncTask<Artist,Void,Void>{

		AsyncAlbumTask mAsyncTaskCallback;
		
		public DownloadAlbumsFromArtistXml(AsyncAlbumTask tc){
			mAsyncTaskCallback=tc;
			
		}
		@Override
		protected Void doInBackground(Artist... params) {
			// TODO Auto-generated method stub
			String urlString="";
			Log.d("DownloadAlbumsFromArtist","artist.getId()="+params[0].getId());
				Artist a = params[0];
				urlString = controller.getServer().getHost() + "/server/xml.server.php?action=artist_albums&auth="
						+ controller.getServer().getAuthKey()+"&filter="+a.getId();
			Log.d("doInBackground",urlString);
				URL url;
				try {
					url = new URL(urlString);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.connect();
					XmlPullParserFactory pullParserFactory;
					try {
						pullParserFactory = XmlPullParserFactory.newInstance();
						XmlPullParser parser = pullParserFactory.newPullParser();
						InputStream in_s = con.getInputStream();
						parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
						Log.d("Albums",in_s.toString()+":"+a.getName());
						parser.setInput(in_s, null);
						parseAlbumsXML(parser);
					} catch (XmlPullParserException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					Log.d("error", "keine Verbindung möglich");
					e.printStackTrace();
				}
			
			return null;
		}
	    @Override
	    protected void onPostExecute(Void result) {
	        mAsyncTaskCallback.onTaskDone(albums);
	    }
		
	}
	
	
	/**
	 * 
	 * @param callback
	 * 
	 * @author Gasperin Anthony
	 */
	public void asyncGetArtist(CallBackGetArtistTask callback){
		new AsyncGetArtistTask(callback).execute();
	}

	
	/**
	 * 
	 * @param callBackGetSongsTask
	 * 
	 * @author Gasperin Anthony
	 */
	public void asyncGetSongs(CallBackGetSongsTask callBackGetSongsTask){
		new AsyncGetSongsTask(callBackGetSongsTask).execute();
	}
	
	/**
	 * 
	 * @author Gasperin Anthony
	 *
	 */
	private class AsyncGetArtistTask extends AsyncTask<Void,Void,Void>{
		CallBackGetArtistTask mAsyncTaskCallback;
		
		public AsyncGetArtistTask(CallBackGetArtistTask callback){
			mAsyncTaskCallback=callback;
			
		}
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			String urlString="";
			Log.d("AsyncGetArtistTask=","Get Some Artist Name ");
					urlString = controller.getServer().getHost() + "/server/xml.server.php?action=artists&auth="
						+ controller.getServer().getAuthKey();
			Log.d("AsyncGetArtistTask",urlString);
				URL url;
				try {
					url = new URL(urlString);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();

					con.getContentLength();
					con.connect();
	
					XmlPullParserFactory pullParserFactory;
					try {

						pullParserFactory = XmlPullParserFactory.newInstance();
						XmlPullParser parser = pullParserFactory.newPullParser();
						InputStream in_s = con.getInputStream();
						
						parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

						parser.setInput(in_s, null);
						parseArtistXML(parser);
			
						
					} catch (XmlPullParserException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					Log.d("error", "keine Verbindung möglich");
					e.printStackTrace();
				}
			
			return null;
		}
	    @Override
	    protected void onPostExecute(Void result) {
	        mAsyncTaskCallback.onTaskDone(artists);
	    }		
	    
	    @Override
	    protected void onPreExecute(){
	    	mAsyncTaskCallback.setProgressVisibility();
	    }
	    
	    @Override
	    protected void onProgressUpdate(Void...params){
	    	mAsyncTaskCallback.updateProgressTask();
	    }
	    
		private void parseArtistXML(XmlPullParser parser) throws XmlPullParserException, IOException {
			artists = new ArrayList<Artist>();
			
			int eventType = parser.getEventType();
			Artist currentArtist = null;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String name = null;
				switch (eventType) {
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equals("artist")) {
						currentArtist = new Artist();
						currentArtist.setId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
		
						
						
					} else if (currentArtist != null) {
						if (name.equals("name")) {
							currentArtist.setName(parser.nextText());
						} else if (name.equals("albums")) {
							currentArtist.setAlbums(Integer.parseInt(parser.nextText()));
						} else if (name.equals("songs")) {
							currentArtist.setSongs(Integer.parseInt(parser.nextText()));
						}
					}
					break;
				case XmlPullParser.END_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase("artist") && currentArtist != null) {
						artists.add(currentArtist);
						progress++;
						publishProgress();
					}
				}
			
				eventType = parser.next();
			}
			Log.d("artists:", artists.toString());
		}
	}
	
	
	
	
	/**
	 * 
	 * @author Gasperin Anthony
	 *
	 */
	private class AsyncGetSongsTask extends AsyncTask<Void,Void,Void>{
		CallBackGetSongsTask mAsyncTaskCallback;
		
		public AsyncGetSongsTask(CallBackGetSongsTask callback){
			mAsyncTaskCallback=callback;
			
		}
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			String urlString="";
			Log.d("AsyncGetSongsTask=","Get Some Songs");
					urlString = controller.getServer().getHost() + "/server/xml.server.php?action=songs&auth="
						+ controller.getServer().getAuthKey();
			Log.d("AsyncGetSongsTask",urlString);
				URL url;
				try {
					url = new URL(urlString);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();

					con.getContentLength();
					con.connect();
	
					XmlPullParserFactory pullParserFactory;
					try {

						pullParserFactory = XmlPullParserFactory.newInstance();
						XmlPullParser parser = pullParserFactory.newPullParser();
						InputStream in_s = con.getInputStream();
						
						parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

						parser.setInput(in_s, null);
						mParseSongsXML(parser);
			
						
					} catch (XmlPullParserException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					Log.d("error", "keine Verbindung möglich");
					e.printStackTrace();
				}
			
			return null;
		}
	    @Override
	    protected void onPostExecute(Void result) {
	        mAsyncTaskCallback.onTaskDone(songs);
	    }		
	    
	    @Override
	    protected void onPreExecute(){
	    	mAsyncTaskCallback.setProgressVisibility();
	    }
	    
	    @Override
	    protected void onProgressUpdate(Void...params){
	    	mAsyncTaskCallback.updateProgressTask();
	    }
	    
		private void mParseSongsXML(XmlPullParser parser) throws XmlPullParserException, IOException {
			songs = new ArrayList<Song>();
			int eventType = parser.getEventType();
			Song currentSong = null;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String name = null;
				switch (eventType) {
				case XmlPullParser.START_TAG:
					name = parser.getName();
					if (name.equals("song")) {
						currentSong = new Song();
						currentSong.setId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
					} else if (currentSong != null) {
						if (name.equals("title")) {
							currentSong.setTitle(parser.nextText());
						} else if (name.equals("artist")) {
							currentSong.setArtist(parser.nextText());
							// currentSong.setArtistId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
						} else if (name.equals("album")) {
							currentSong.setAlbum(parser.nextText());
							// currentSong.setAlbumId(Integer.parseInt((parser.getAttributeValue(null, "id"))));
						} else if (name.equals("track")) {
							currentSong.setTrackNumber(Integer.parseInt(parser.nextText()));
						} else if (name.equals("time")) {
							currentSong.setTime(Integer.parseInt(parser.nextText()));
						} else if (name.equals("year")) {
							currentSong.setYear(Integer.parseInt(parser.nextText()));
						} else if (name.equals("bitrate")) {
							currentSong.setBitrate(Integer.parseInt(parser.nextText()));
						} else if (name.equals("mode")) {
							currentSong.setMode(parser.nextText());
						} else if (name.equals("mime")) {
							currentSong.setMime(parser.nextText());
						} else if (name.equals("url")) {
							currentSong.setUrl(parser.nextText());
						} else if (name.equals("size")) {
							currentSong.setSize(Integer.parseInt(parser.nextText()));
						} else if (name.equals("art")) {
							currentSong.setArt(parser.nextText());
						}
					}
					break;
				case XmlPullParser.END_TAG:
					name = parser.getName();
					if (name.equalsIgnoreCase("song") && currentSong != null) {
						songs.add(currentSong);
						progress++;
						publishProgress();
					}
				}
				eventType = parser.next();
			}
			Log.d("songs:", songs.toString());
		}
	}

	/**
	 * 
	 * @author Gasperin Anthony
	 *
	 */
	public interface CallBackGetArtistTask{
		
		public void onTaskDone(ArrayList<Artist> listArtist);
		
		public void updateProgressTask();
		
		public void setProgressVisibility();
		
		
	}
	
	
	/**
	 * 
	 * @author Gasperin Anthony
	 *
	 */
	public interface CallBackGetSongsTask{
		
		public void onTaskDone(ArrayList<Song> listSongs);
		
		public void updateProgressTask();
		
		public void setProgressVisibility();
		
		
	}
}
