/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014 Daniel Schruhl, 2015 Gasperin Anthony
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
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
import com.anaximandre.ampache.Song;
import com.anaximandre.zampache.AlbumArrayAdapter;
import com.anaximandre.zampache.Controller;
import com.anaximandre.zampache.MainActivity;
import com.anaximandre.zampache.R;

/**
 * @author Daniel Schruhl
 * 
 */
public class AlbumsView extends Fragment {

	private Controller controller;
	public static String SONGS_VIEW_FRAGMENT="current_songs_fragement";
	/**
	 * 
	 */
	public AlbumsView() {
		// TODO Auto-generated constructor stub
	}

	public static Fragment newInstance(Context context) {
		AlbumsView p = new AlbumsView();
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
			for (Album a : controller.getAlbums()) {
				list.add(a.toString());
			}
			AlbumArrayAdapter adapter = new AlbumArrayAdapter(getActivity().getApplicationContext(), list,
					controller.getAlbums());
			listview.setAdapter(adapter);
			registerForContextMenu(listview);

			listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
					Album a = controller.getAlbums().get(position);
					controller.getSelectedSongs().clear();
					
					
					controller.findSongs(a,new AsyncTaskCallback(){
						public void onTaskDone(ArrayList<Song> s){
							setAlbumSongs(s);
						}
					});
					/*for (Song s : controller.findSongs(a)) {
						Log.d("Song title=",s.getTitle());
						controller.getSelectedSongs().add(s);
					}*/
					// Create new fragment and transaction
					SelectedSongsView newFragment = new SelectedSongsView();
					FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();

					// Replace whatever is in the fragment_container view with this fragment,
					// and add the transaction to the back stack
					transaction.replace(R.id.content_frame, newFragment,SONGS_VIEW_FRAGMENT);
					transaction.addToBackStack(null);
					((MainActivity) getActivity()).setActiveFragment(6);
					// Commit the transaction
					transaction.commit();
				}

			});
		}
		return root;
	}
	
	private void setAlbumSongs(ArrayList<Song> songs){
		for (Song s : songs) {
			Log.d("Song title=",s.getTitle());
			controller.getSelectedSongs().add(s);
		}
		SelectedSongsView currentFrag = (SelectedSongsView)getFragmentManager().findFragmentByTag(SONGS_VIEW_FRAGMENT);
		 getActivity().getSupportFragmentManager()
         .beginTransaction()
         .detach(currentFrag)
         .attach(currentFrag)
         .commit();
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
		Album a = controller.getAlbums().get((int) info.id);
		switch (item.getItemId()) {
		case R.id.contextMenuAdd:
			for (Song s : controller.findSongs(a)) {
				controller.getPlayNow().add(s);
			}
			Context context = getView().getContext();
			CharSequence text = getResources().getString(R.string.albumsViewAlbumsAdded);
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
	
	
	public interface AsyncTaskCallback{
		public void onTaskDone(ArrayList<Song> s);
	}
}
