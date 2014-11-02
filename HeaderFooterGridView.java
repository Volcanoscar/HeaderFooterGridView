/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Inspired by {@link ListView}
 */

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

/**
 * A {@link GridView} that supports adding single header row/single footer row in a very similar way to {@link ListView}.
 */
public class HeaderFooterGridView extends GridView {

  private static final String TAG = "HeaderFooterGridView";

  /**
   * A class that represents a fixed view in a list, for example a header at the top or a footer at the bottom.
   */
  private static class FixedViewInfo {

    /**
     * The view to add to the grid
     */
    public View view;

    public ViewGroup viewContainer;

    /**
     * The data backing the view. This is returned from {@link ListAdapter#getItem(int)}.
     */
    public Object data;

    /**
     * <code>true</code> if the fixed view should be selectable in the grid
     */
    public boolean isSelectable;
  }

  private FixedViewInfo mHeaderViewInfo = null;

  private FixedViewInfo mFooterViewInfo = null;

  private void initHeaderGridView() {
    super.setClipChildren(false);
  }

  public HeaderFooterGridView(Context context) {
    this(context, null);
  }

  public HeaderFooterGridView(Context context, AttributeSet attrs) {
    this(context,attrs,0);
  }

  public HeaderFooterGridView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    initHeaderGridView();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    ListAdapter adapter = getAdapter();
    if (adapter != null && adapter instanceof HeaderFooterGridViewAdapter) {
      ((HeaderFooterGridViewAdapter) adapter).setNumColumns(getNumColumns());
    }
  }

  @Override
  public void setClipChildren(boolean clipChildren) {
    // Ignore, since the header rows depend on not being clipped
  }

  /**
   * Add a fixed view to appear at the top of the grid. <p> NOTE: Call this before calling setAdapter. 
   *
   * @param v            The view to add.
   * @param data         Data to associate with this view
   * @param isSelectable whether the item is selectable
   */
  public void addHeaderView(View v, Object data, boolean isSelectable) {
    ListAdapter adapter = getAdapter();
    if (adapter != null && !(adapter instanceof HeaderFooterGridViewAdapter)) {
      throw new IllegalStateException(
          "Cannot add header view to grid -- setAdapter has already been called.");
    }
    mHeaderViewInfo = new FixedViewInfo();
    FrameLayout fl = new FullWidthFixedViewLayout(getContext());
    fl.addView(v);
    mHeaderViewInfo.view = v;
    mHeaderViewInfo.viewContainer = fl;
    mHeaderViewInfo.data = data;
    mHeaderViewInfo.isSelectable = isSelectable;

    // in the case of re-adding a header view, or adding one later on,
    // we need to notify the observer
    if (adapter != null) {
      ((HeaderFooterGridViewAdapter) adapter).notifyDataSetChanged();
    }
  }

  public void addFooterView(View v, Object data, boolean isSelectable) {
    ListAdapter adapter = getAdapter();
    if (adapter != null && !(adapter instanceof HeaderFooterGridViewAdapter)) {
      throw new IllegalStateException(
          "Cannot add header view to grid -- setAdapter has already been called.");
    }

    mFooterViewInfo = new FixedViewInfo();
    FrameLayout fl = new FullWidthFixedViewLayout(getContext());
    fl.addView(v);
    mFooterViewInfo.view = v;
    mFooterViewInfo.viewContainer = fl;
    mFooterViewInfo.data = data;
    mFooterViewInfo.isSelectable = isSelectable;

    // in the case of re-adding a header view, or adding one later on,
    // we need to notify the observer
    if (adapter != null) {
      ((HeaderFooterGridViewAdapter) adapter).notifyDataSetChanged();
    }
  }

  /**
   * Add a fixed view to appear at the top of the grid. <p> NOTE: Call this before calling setAdapter. 
   *
   * @param v            The view to add.
   * @param data         Data to associate with this view
   * @param isSelectable whether the item is selectable
   */
  public void addHeaderView(View v) {
    addHeaderView(v, null, false);
  }

  public void addHeaderView(View v, boolean isSelectable) {
    addHeaderView(v, null, isSelectable);
  }

  public void addFooterView(View v) {
    addFooterView(v, null, false);
  }

  /**
   * Removes a previously-added header view.
   *
   * @param v The view to remove
   * @return true if the view was removed, false if the view was not a header view
   */
  public boolean removeHeaderView(View v) {
    if (mHeaderViewInfo != null) {
      boolean result = false;
      ListAdapter adapter = getAdapter();
      if (adapter != null && ((HeaderFooterGridViewAdapter) adapter).removeHeader(v)) {
        result = true;
      }
      mHeaderViewInfo = null;
      return result;
    }
    return false;
  }

  public boolean removeFooterView(View v) {
    if (mFooterViewInfo != null) {
      boolean result = false;
      ListAdapter adapter = getAdapter();
      if (adapter != null && ((HeaderFooterGridViewAdapter) adapter).removeFooter(v)) {
        result = true;
      }
      mFooterViewInfo = null;
      return result;
    }
    return false;
  }

  public boolean hasHeaderView() {
    return mHeaderViewInfo != null;
  }

  public boolean hasFooterView() {
    return mFooterViewInfo != null;
  }

  @Override
  public void setAdapter(ListAdapter adapter) {
    if ((mHeaderViewInfo != null || mFooterViewInfo != null) && adapter != null) {
      HeaderFooterViewGridAdapter hAdapter = new HeaderFooterViewGridAdapter(mHeaderViewInfo, mFooterViewInfo, adapter);
      int numColumns = getNumColumns();
      if (numColumns > 1) {
        hAdapter.setNumColumns(numColumns);
      }
      super.setAdapter(hAdapter);
    } else {
      super.setAdapter(adapter);
    }
  }

  private class FullWidthFixedViewLayout extends FrameLayout {

    public FullWidthFixedViewLayout(Context context) {
      super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      int targetWidth = HeaderFooterGridView.this.getMeasuredWidth()
          - HeaderFooterGridView.this.getPaddingLeft()
          - HeaderFooterGridView.this.getPaddingRight();
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth,
          MeasureSpec.getMode(widthMeasureSpec));
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
  }

  /**
   * ListAdapter used when a HeaderGridView has header views. This ListAdapter wraps another one and also keeps track of the header views and their
   * associated data objects. <p>This is intended as a base class; you will probably not need to use this class directly in your own code.
   */
  private static class HeaderFooterGridViewAdapter implements WrapperListAdapter, Filterable {

    // This is used to notify the container of updates relating to number of columns
    // or headers changing, which changes the number of placeholders needed
    private final DataSetObservable mDataSetObservable = new DataSetObservable();

    private final ListAdapter mAdapter;

    private int mNumColumns = 1;

    FixedViewInfo mHeaderViewInfos;

    FixedViewInfo mFooterViewInfos;

    boolean mAreAllFixedViewsSelectable;

    private final boolean mIsFilterable;

    private boolean heightFlag = false;

    private int minActualViewHeight;

    public HeaderFooterGridViewAdapter(FixedViewInfo headerViewInfos, FixedViewInfo footerViewInfos,
        ListAdapter adapter) {
      mAdapter = adapter;
      mIsFilterable = adapter instanceof Filterable;
      mHeaderViewInfos = headerViewInfos;
      mFooterViewInfos = footerViewInfos;
      mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos) &&
          areAllListInfosSelectable(mFooterViewInfos);
    }

    public int getHeadersCount() {
      return mHeaderViewInfos == null ? 0 : 1;
    }

    public int getFootersCount() {
      return mFooterViewInfos == null ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
      return (mAdapter == null || mAdapter.isEmpty()) && getHeadersCount() == 0 && getFootersCount() == 0;
    }

    public void setNumColumns(int numColumns) {
      if (numColumns < 1) {
        throw new IllegalArgumentException("Number of columns must be 1 or more");
      }
      if (mNumColumns != numColumns) {
        mNumColumns = numColumns;
        notifyDataSetChanged();
      }
    }

    private boolean areAllListInfosSelectable(FixedViewInfo infos) {
      if (infos != null) {
        if (!infos.isSelectable) {
          return false;
        }
      }
      return true;
    }

    public boolean removeHeader(View v) {
      if (mHeaderViewInfos != null) {
        if (mHeaderViewInfos.view == v) {
          mHeaderViewInfos = null;
          mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos);
          mDataSetObservable.notifyChanged();
          return true;
        }
      }

      return false;
    }

    public boolean removeFooter(View v) {
      if (mFooterViewInfos != null) {
        if (mFooterViewInfos.view == v) {
          mFooterViewInfos = null;
          mAreAllFixedViewsSelectable = areAllListInfosSelectable(mFooterViewInfos);
          mDataSetObservable.notifyChanged();
          return true;
        }
      }

      return false;
    }

    @Override
    public int getCount() {
      if (mAdapter != null) {
        return (getFootersCount() + getHeadersCount()) * mNumColumns + mAdapter.getCount();
      } else {
        return (getFootersCount() + getHeadersCount()) * mNumColumns;
      }
    }

    @Override
    public boolean areAllItemsEnabled() {
      if (mAdapter != null) {
        return mAreAllFixedViewsSelectable && mAdapter.areAllItemsEnabled();
      } else {
        return true;
      }
    }

    @Override
    public boolean isEnabled(int position) {
      // Header/Footer (negative positions will throw an ArrayIndexOutOfBoundsException)
      int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
      if (position < numHeadersAndPlaceholders) {
        return (mHeaderViewInfos == null || mHeaderViewInfos.isSelectable);
      }

      // Adapter
      final int adjPosition = position - numHeadersAndPlaceholders;
      int adapterCount = 0;
      if (mAdapter != null) {
        adapterCount = mAdapter.getCount();
        if (adjPosition < adapterCount) {
          return mAdapter.isEnabled(adjPosition);
        }
      }

      return (mFooterViewInfos == null || mFooterViewInfos.isSelectable);
    }

    @Override
    public Object getItem(int position) {
      // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
      int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
      if (position < numHeadersAndPlaceholders) {
        if (position % mNumColumns == 0) {
          return mHeaderViewInfos.data;
        }
        return null;
      } else {
        int adjPosition = position - numHeadersAndPlaceholders;
        int adapterCount = 0;
        if (mAdapter != null) {
          adapterCount = mAdapter.getCount();
        }
        if (adjPosition < adapterCount && mAdapter != null) {
          return mAdapter.getItem(adjPosition);
        } else {
          if (mFooterViewInfos != null) {
            return mFooterViewInfos.data;
          }
        }
      }
      throw new ArrayIndexOutOfBoundsException(position);
    }

    @Override
    public long getItemId(int position) {
      int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
      if (mAdapter != null && position >= numHeadersAndPlaceholders) {
        int adjPosition = position - numHeadersAndPlaceholders;
        int adapterCount = mAdapter.getCount();
        if (adjPosition < adapterCount) {
          return mAdapter.getItemId(adjPosition);
        }
      }
      return -1;
    }

    @Override
    public boolean hasStableIds() {
      if (mAdapter != null) {
        return mAdapter.hasStableIds();
      }
      return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
      int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;

      if (position < numHeadersAndPlaceholders) {
        View headerViewContainer = mHeaderViewInfos.viewContainer;
        if (position % mNumColumns == 0) {
          return headerViewContainer;
        } else {
          if (convertView == null) {
            convertView = new View(parent.getContext());
          }
          // We need to do this because GridView uses the height of the last item
          // in a row to determine the height for the entire row.
          convertView.setVisibility(View.INVISIBLE);
          convertView.setMinimumHeight(headerViewContainer.getHeight());
          return convertView;
        }
      } else {
        int adjPosition = position - numHeadersAndPlaceholders;
        int adapterCount = 0;
        if (mAdapter != null) {
          adapterCount = mAdapter.getCount();
        }
        if (adjPosition < adapterCount && mAdapter != null) {
          View view = mAdapter.getView(adjPosition, convertView, parent);
          minActualViewHeight = view.getHeight();
          heightFlag = false;
          return view;
        } else {
          View footerViewContainer = mFooterViewInfos.viewContainer;
          if (position % mNumColumns == 0) {
            heightFlag = true;
            return footerViewContainer;
          } else {
            if (convertView == null) {
              convertView = new View(parent.getContext());
            }
            // We need to do this because GridView uses the height of the last item
            // in a row to determine the height for the entire row.
            convertView.setVisibility(View.INVISIBLE);
            if (!heightFlag) {
              convertView.setMinimumHeight(minActualViewHeight);
            } else {
              convertView.setMinimumHeight(mFooterViewInfos.viewContainer.getHeight());
            }
            return convertView;
          }
        }
      }
    }

    @Override
    public int getItemViewType(int position) {
      int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
      if (position < numHeadersAndPlaceholders && (position % mNumColumns != 0)) {
        // Placeholders get the last view type number
        return mAdapter != null ? mAdapter.getViewTypeCount() : 1;
      }
      if (mAdapter != null && position >= numHeadersAndPlaceholders) {
        int adjPosition = position - numHeadersAndPlaceholders;
        int adapterCount = mAdapter.getCount();
        if (adjPosition < adapterCount) {
          return mAdapter.getItemViewType(adjPosition);
        }
      }

      return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
    }

    @Override
    public int getViewTypeCount() {
      if (mAdapter != null) {
        return mAdapter.getViewTypeCount() + 1;
      }
      return 2;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
      mDataSetObservable.registerObserver(observer);
      if (mAdapter != null) {
        mAdapter.registerDataSetObserver(observer);
      }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
      mDataSetObservable.unregisterObserver(observer);
      if (mAdapter != null) {
        mAdapter.unregisterDataSetObserver(observer);
      }
    }

    @Override
    public Filter getFilter() {
      if (mIsFilterable) {
        return ((Filterable) mAdapter).getFilter();
      }
      return null;
    }

    @Override
    public ListAdapter getWrappedAdapter() {
      return mAdapter;
    }

    public void notifyDataSetChanged() {
      mDataSetObservable.notifyChanged();
    }
  }
}
