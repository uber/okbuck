/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.okbuilds.okbuck.example;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;

/**
 * Created by Piasy{github.com/Piasy} on 15/10/2.
 */
public class CollapsingAppBarActivity extends AppCompatActivity {
    RecyclerView mRecyclerView;
    AppBarLayout mAppBarLayout;
    CoordinatorLayout mCoordinatorLayout;

    private int mAppBarHeight = -1;
    private int mCurrentAppBarOffset;   // -height ==> 0
    private final float APP_BAR_AUTO_COLLAPSE_RATION = 0.3F;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collapsing_app_bar);
        bind();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        Adapter adapter = new Adapter(this, new Adapter.Action() {
            @Override
            public void onClick() {
                mAppBarLayout.setExpanded(false);
            }
        });
        mRecyclerView.setAdapter(adapter);
        adapter.setContentCount(40);

        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (mAppBarHeight == -1) {
                    mAppBarHeight = appBarLayout.getHeight();
                }
                mCurrentAppBarOffset = -verticalOffset;
            }
        });
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if ((float) mCurrentAppBarOffset / mAppBarHeight <
                            APP_BAR_AUTO_COLLAPSE_RATION) {
                        mAppBarLayout.setExpanded(true);
                    } else {
                        mAppBarLayout.setExpanded(false);
                    }
                }
            }
        });
    }

    private void bind() {
        mRecyclerView = ButterKnife.findById(this, R.id.mRecyclerView);
        mAppBarLayout = ButterKnife.findById(this, R.id.mAppBarLayout);
        mCoordinatorLayout = ButterKnife.findById(this, R.id.mCoordinatorLayout);
    }

    public static class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private final Context mContext;
        private final Action mAction;

        public Adapter(Context context, Action action) {
            mContext = context;
            mAction = action;
        }

        private int mContentCount = 0;

        public void setContentCount(int contentCount) {
            mContentCount = contentCount;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.ui_recycler_view_horizontal_item, parent, false));
        }

        public interface Action {
            void onClick();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            holder.mTextView.setText(String.valueOf(position));
            holder.mLlContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(mContext, "Click: " + position, Toast.LENGTH_SHORT).show();
                    mAction.onClick();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mContentCount;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView mTextView;
        LinearLayout mLlContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.mNumber);
            mLlContainer = (LinearLayout) itemView.findViewById(R.id.mLlContainer);
        }
    }
}
