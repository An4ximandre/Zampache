/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Daniel Schruhl, 2015 Anthony Gasperin.
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
package com.anaximandre.zampache.views;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.anaximandre.ampache.Album;
import com.anaximandre.ampache.Artist;
import com.anaximandre.ampache.Song;
import com.anaximandre.zampache.ArtistArrayAdapter;
import com.anaximandre.zampache.Controller;
import com.anaximandre.zampache.R;

//import com.racoon.ampdroid.ServerConnector;

/**
 * @author Daniel Schruhl, 2015 Anthony Gasperin
 * 
 */
public class ArtistsView extends Fragment {

	// private String urlString;
	private Controller controller;
	public static String ALBUMS_VIEW_FRAGMENT="albums_view_fragment";
	/**
	 * 
	 */
	public ArtistsView() {
		// TODO Auto-generated constructor stub
	}

	public static Fragment newInstance(Context context) {
		ArtistsView p = new ArtistsView();
		return p;
	}

	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		controller = Controller.getInstance();
		ViewGroup root = (ViewGroup) inflater.inflate(R.layout.ampache_songs, null);
		ListView listview = (ListView) root.findViewById(R.id.songs_listview);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			listview.setFastScrollAlwaysVisible(true);
		}
		if (controller.getServer() != null) {
			ArrayList<String> list = new ArrayList<String>();
			for (Artist a : controller.getArtists()) {	
				list.add(a.toString());
			}
			ArtistArrayAdapter adapter = new ArtistArrayAdapter(getActivity().getApplicationContext(), list,
					controller.getArtists());
			listview.setAdapter(adapter);
			registerForContextMenu(listview);

			listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
					Artist artist = controller.getArtists().get(position);

					controller.getSelectedAlbums().clear();
					/*for (Album a : controller.findAlbums(artist)) {
						controller.getSelectedAlbums().add(a);
					}*/
					controller.findAlbums(artist,new AsyncAlbumTask(){

						@Override
						public void onTaskDone(ArrayList<Album> a) {
							// TODO Auto-generated method stub
							updateAlbumsView(a);
							
						}
						
					});
					// Create new fragment and transaction
					SelectedAlbumsView newFragment = new SelectedAlbumsView();

					// controller.getSelectedSongs().clear();
					// for (Song s : controller.findSongs(a)) {
					// controller.getSelectedSongs().add(s);
					// }
					// // Create new fragment and transaction
					// SelectedSongsView newFragment = new SelectedSongsView();
					FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

					// Replace whatever is in the fragment_container view with this fragment,
					// and add the transaction to the back stack
					transaction.replace(R.id.content_frame, newFragment,ALBUMS_VIEW_FRAGMENT);
					transaction.addToBackStack(null);
					// ((MainActivity) getActivity()).setActiveFragment(6);
					// Commit the transaction
					transaction.commit();
				}

			});
		}
		return root;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.context_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Artist a = controller.getArtists().get((int) info.id);
		switch (item.getItemId()) {
		case R.id.contextMenuAdd:
			for (Song s : controller.findSongs(a)) {
				controller.getPlayNow().add(s);
			}
			Context context = getView().getContext();
			CharSequence text = getResources().getString(R.string.artistsViewArtistSongsAdded);
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			return true;
		case R.id.contextMenuOpen:
			controller.getSelectedSongs().clear();
			for (Song s : controller.findSongs(a)) {
				controller.getSelectedSongs().add(s);
			}
			// Create new fragment and transaction
			SelectedSongsView newFragment = new SelectedSongsView();
			FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

			// Replace whatever is in the fragment_container view with this fragment,
			// and add the transaction to the back stack
			transaction.replace(R.id.content_frame, newFragment);
			transaction.addToBackStack(null);

			// Commit the transaction
			transaction.commit();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	public void updateAlbumsView(ArrayList<Album> albums){
		for (Album a : albums) {
			Log.d("Song title=",a.getName());
			controller.getSelectedAlbums().add(a);
		}
		try{
			SelectedAlbumsView currentFrag = (SelectedAlbumsView)getFragmentManager().findFragmentByTag(ALBUMS_VIEW_FRAGMENT);
			if (currentFrag !=null){
				getActivity().getSupportFragmentManager()
				.beginTransaction()
				.detach(currentFrag)
				.attach(currentFrag)
				.commit();
			}
		}catch(NullPointerException ne){Log.e("UpdateAlbumsView","Fragment not found");}
	}
	
	public interface AsyncAlbumTask{
		public void onTaskDone(ArrayList<Album> a);
	}
}
