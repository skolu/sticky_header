package com.example.recyclerview.app;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment(), "BODY")
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_scroll) {
            PlaceholderFragment f = (PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("BODY");
            if (f != null) {
                f.mRecycler.scrollToPosition(50);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static abstract class TestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements StickyHeaderLayout.IHeaderPosition{}

    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        RecyclerView mRecycler;
        TestAdapter mAdapter;
        RecyclerView.LayoutManager mLayoutManager;

        private static final int ROW_VIEW_TYPE = 0;
        private static final int HEADER_VIEW_TYPE = 1;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mRecycler = (RecyclerView) view.findViewById(R.id.recycler);

            mAdapter = new TestAdapter () {
                @Override
                public int getHeaderPosition(int position) {
                    return position - (position % 10);
                }

                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    switch (viewType) {
                        case ROW_VIEW_TYPE: {
                            View view = getActivity().getLayoutInflater().inflate(R.layout.text_cell, parent, false);
                            return new RowViewHolder(view);
                        }
                        case HEADER_VIEW_TYPE: {
                            View view = getActivity().getLayoutInflater().inflate(R.layout.header_cell, parent, false);
                            return new HeaderViewHolder(view);
                        }
                        default:
                            return null;
                    }
                }

                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                    if (holder instanceof HeaderViewHolder) {
                        HeaderViewHolder vh = (HeaderViewHolder) holder;
                        vh.mTextView.setText(String.format("Header: %d", position));
                    }
                    else if (holder instanceof RowViewHolder) {
                        RowViewHolder vh = (RowViewHolder) holder;
                        vh.mTextView.setText(String.format("Row: %d", position));
                    }
                }

                @Override
                public int getItemCount() {
                    return 101;
                }

                @Override
                public int getItemViewType(int position) {
                    return (position % 10) == 0 ? HEADER_VIEW_TYPE : ROW_VIEW_TYPE;
                }
            };

            mRecycler.setAdapter(mAdapter);

            //mLayoutManager = new LinearLayoutManager(getActivity());
            mLayoutManager = new StickyHeaderLayout(mAdapter);
            mRecycler.setLayoutManager(mLayoutManager);
        }

        class RowViewHolder extends RecyclerView.ViewHolder {
            TextView mTextView;
            public RowViewHolder(View itemView) {
                super(itemView);

                mTextView = (TextView) itemView.findViewById(R.id.text);
            }
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView mTextView;
            public HeaderViewHolder(View itemView) {
                super(itemView);

                mTextView = (TextView) itemView.findViewById(R.id.text);
            }
        }
    }
}
