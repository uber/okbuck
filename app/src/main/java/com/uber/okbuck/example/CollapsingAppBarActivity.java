package com.uber.okbuck.example;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.AppBarLayout;

public class CollapsingAppBarActivity extends AppCompatActivity {
  private final float APP_BAR_AUTO_COLLAPSE_RATION = 0.3F;
  RecyclerView mRecyclerView;
  AppBarLayout mAppBarLayout;
  CoordinatorLayout mCoordinatorLayout;
  private int mAppBarHeight = -1;
  private int mCurrentAppBarOffset; // -height ==> 0

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_collapsing_app_bar);
    bind();
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    Adapter adapter =
        new Adapter(
            this,
            new Adapter.Action() {
              @Override
              public void onClick() {
                mAppBarLayout.setExpanded(false);
              }
            });
    mRecyclerView.setAdapter(adapter);
    adapter.setContentCount(40);

    mAppBarLayout.addOnOffsetChangedListener(
        new AppBarLayout.OnOffsetChangedListener() {
          @Override
          public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            if (mAppBarHeight == -1) {
              mAppBarHeight = appBarLayout.getHeight();
            }
            mCurrentAppBarOffset = -verticalOffset;
          }
        });
    mRecyclerView.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
              if ((float) mCurrentAppBarOffset / mAppBarHeight < APP_BAR_AUTO_COLLAPSE_RATION) {
                mAppBarLayout.setExpanded(true);
              } else {
                mAppBarLayout.setExpanded(false);
              }
            }
          }
        });
  }

  private void bind() {
    mRecyclerView = findViewById(R.id.mRecyclerView);
    mAppBarLayout = findViewById(R.id.mAppBarLayout);
    mCoordinatorLayout = findViewById(R.id.mCoordinatorLayout);
  }

  public static class Adapter extends RecyclerView.Adapter<ViewHolder> {

    private final Context mContext;
    private final Action mAction;
    private int mContentCount = 0;

    public Adapter(Context context, Action action) {
      mContext = context;
      mAction = action;
    }

    public void setContentCount(int contentCount) {
      mContentCount = contentCount;
      notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return new ViewHolder(
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.ui_recycler_view_horizontal_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
      holder.mTextView.setText(String.valueOf(holder.getAdapterPosition()));
      holder.mLlContainer.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Toast.makeText(mContext, "Click: " + holder.getAdapterPosition(), Toast.LENGTH_SHORT)
                  .show();
              mAction.onClick();
            }
          });
    }

    @Override
    public int getItemCount() {
      return mContentCount;
    }

    public interface Action {
      void onClick();
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
