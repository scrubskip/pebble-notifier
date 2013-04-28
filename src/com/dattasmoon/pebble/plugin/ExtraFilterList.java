package com.dattasmoon.pebble.plugin;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

/**
 * Displays the list of extra filters.
 */
public class ExtraFilterList extends FragmentActivity {

    private SharedPreferences mSharedPreferences;
    private List<Filter>      mFilters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = getSharedPreferences(Constants.LOG_TAG, MODE_MULTI_PROCESS | MODE_PRIVATE);
        setContentView(R.layout.activity_extra_filters);
        addListFragment();
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("edit");
        if (fragment != null) {
            Filter filter = ((EditFilterFragment) fragment).getFilter();
            int index = ((EditFilterFragment) fragment).getIndex();
            if (filter != null) {
                if (index == -1) {
                    mFilters.add(filter);
                } else {
                    mFilters.set(index, filter);
                }
            } else if (index != -1) {
                // Deleting.
                mFilters.remove(index);
            }
            saveFilters();
        }
        super.onBackPressed();
    }

    void addListFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.root, FilterFragment.newInstance(), "list");
        ft.commit();
    }

    void addEditFragment(int index) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        EditFilterFragment fragment;
        if (index == -1) {
            fragment = EditFilterFragment.newInstance();
        } else {
            fragment = EditFilterFragment.newInstance(mFilters.get(index), index);
        }
        ft.replace(R.id.root, fragment, "edit");
        ft.addToBackStack("edit");
        ft.commit();
    }

    void setFilters(List<Filter> filters) {
        mFilters = filters;
    }

    private void saveFilters() {
        mSharedPreferences.edit().putString(Constants.PREFERENCE_EXTRA_FILTERS, Filter.toJsonString(mFilters)).apply();
    }

    public static class FilterFragment extends ListFragment {

        public static FilterFragment newInstance() {
            return new FilterFragment();
        }

        @Override
        public void onResume() {
            super.onResume();
            getListView().setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    ((ExtraFilterList) getActivity()).addEditFragment((int) arg3);
                }
            });
            new LoadFiltersTask().execute();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_filter_list, container, false);
            Button addNew = (Button) view.findViewById(R.id.add_new);
            addNew.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    ((ExtraFilterList) getActivity()).addEditFragment(-1);
                }
            });
            return view;
        }

        class LoadFiltersTask extends AsyncTask<Void, Void, List<Filter>> {

            @Override
            protected void onPostExecute(List<Filter> result) {
                ArrayAdapter<Filter> adapter = new ArrayAdapter<Filter>(getActivity(), R.layout.list_filter_item,
                        R.id.filter_name, result);
                ((ExtraFilterList) getActivity()).setFilters(result);
                setListAdapter(adapter);
            }

            @Override
            protected List<Filter> doInBackground(Void... arg0) {
                return Filter.fromJsonString(((ExtraFilterList) getActivity()).mSharedPreferences.getString(
                        Constants.PREFERENCE_EXTRA_FILTERS, ""));
            }
        }
    }

    public static class EditFilterFragment extends Fragment {

        private EditText mTextFilter;
        private EditText mTitleFilter;
        private Filter   mFilter;
        private int      mIndex;

        public static EditFilterFragment newInstance() {
            return EditFilterFragment.newInstance(new Filter(), -1);
        }

        public static EditFilterFragment newInstance(Filter filter, int index) {
            Bundle args = new Bundle();
            if (filter != null) {
                args.putParcelable("filter", filter);
                args.putInt("index", index);
            }
            EditFilterFragment fragment = new EditFilterFragment();
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mFilter = getArguments().getParcelable("filter");
            mIndex = getArguments().getInt("index");
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_edit_filter, container, false);
            mTitleFilter = (EditText) view.findViewById(R.id.title_filter);
            mTitleFilter.setText(mFilter.getTitleFilterString());
            mTitleFilter.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable arg0) {
                    Pattern pattern = compilePattern(arg0.toString());
                    if (pattern != null) {
                        mFilter.setTitleFilter(pattern);
                        mTitleFilter.setError(null);
                    } else {
                        mTitleFilter.setError("Invalid regex.");
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                }

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                }

            });
            mTextFilter = (EditText) view.findViewById(R.id.text_filter);
            mTextFilter.setText(mFilter.getTextFilterString());
            mTextFilter.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable arg0) {
                    Pattern pattern = compilePattern(arg0.toString());
                    if (pattern != null) {
                        mFilter.setTextFilter(pattern);
                        mTextFilter.setError(null);
                    } else {
                        mTextFilter.setError("Invalid regex.");
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                }

                @Override
                public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                }
            });
            view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mFilter = null;
                    ((ExtraFilterList) getActivity()).onBackPressed();
                }
            });

            return view;
        }

        public Filter getFilter() {
            return mFilter;
        }

        public int getIndex() {
            return mIndex;
        }

        private Pattern compilePattern(String string) {
            try {
                return Pattern.compile(string);
            } catch (PatternSyntaxException e) {
                return null;
            }
        }
    }
}
