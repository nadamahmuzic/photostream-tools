/*
 * The MIT License
 *
 * Copyright (c) 2016 Andreas Schattney
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hochschuledarmstadt.photostream_tools.examples;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import hochschuledarmstadt.photostream_tools.adapter.DividerItemDecoration;
import hochschuledarmstadt.photostream_tools.examples.comment.CommentActivity;
import hochschuledarmstadt.photostream_tools.examples.photo.PhotoActivity;

public class MenuActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    public static final String MENU_PHOTOS = "Photos";
    public static final String MENU_COMMENTS = "Comments";
    private static final String[] menu = new String[]{
            MENU_PHOTOS,
            MENU_COMMENTS
    };

    private MenuAdapter menuAdapter;

    private static final HashMap<String, Class<?>> menuToClassMap = new HashMap<>();

    static {
        menuToClassMap.put(MENU_PHOTOS, PhotoActivity.class);
        menuToClassMap.put(MENU_COMMENTS, CommentActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this));
        menuAdapter = new MenuAdapter(this, Arrays.asList(menu), new MenuAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                final String menuItem = menuAdapter.getItemAtPosition(position);
                Intent intent = new Intent(MenuActivity.this, menuToClassMap.get(menuItem));
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(menuAdapter);
    }

    private static class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.MenuViewHolder>{

        private final List<String> menu;
        private final Context context;
        private final OnItemClickListener itemClickListener;

        public MenuAdapter(Context context, List<String> menu, OnItemClickListener itemClickListener){
            this.context = context;
            this.menu = menu;
            this.itemClickListener = itemClickListener;
        }

        @Override
        public MenuViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MenuViewHolder(LayoutInflater.from(context).inflate(R.layout.menu_item, parent, false), itemClickListener);
        }

        @Override
        public void onBindViewHolder(MenuViewHolder holder, int position) {
            holder.textView.setText(menu.get(position));
        }

        @Override
        public int getItemCount() {
            return menu.size();
        }

        public String getItemAtPosition(int position) {
            return menu.get(position);
        }

        static class MenuViewHolder extends RecyclerView.ViewHolder{
            public TextView textView;
            public MenuViewHolder(View itemView, final OnItemClickListener itemClickListener) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.textView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        itemClickListener.onItemClick(getAdapterPosition());
                    }
                });
            }
        }

        public interface OnItemClickListener{
            void onItemClick(int position);
        }

    }

}
