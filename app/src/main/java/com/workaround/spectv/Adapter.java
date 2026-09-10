package com.workaround.spectv;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class Adapter extends ArrayAdapter <EpgMapData> implements Filterable {

    ArrayList <EpgMapData> miniGuideList = new ArrayList <>();
    ArrayList <EpgMapData> mOriginalValues = null;
    private OnDataSetChangedListener listener;

    public Adapter(Context context, int textViewResourceId, ArrayList <EpgMapData> objects) {
        super(context, textViewResourceId, objects);
        miniGuideList = objects;
    }

    // Returns total number of items to be displayed in the list.
    // It counts the value from the arraylist size
    @Override
    public int getCount() {
        return miniGuideList.size();
    //    return super.getCount();
    }

    // This function implicitly gets called when the listItem view is ready
    // to be displayed. Here we set the layout and add data to the views
    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        MyViewHolder holder = null;
        EpgMapData epgitem = miniGuideList.get(position);
  //      Log.d("SPECDEBUG", "Adaptor pos = " + position + " chnum = " + epgitem.chnum);

        // Setting layout
        if ( view == null ) {
            Log.d("SPECDEBUG", "Adaptor inflating view");
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.list_item, null);
            holder = new MyViewHolder();
            holder.imageView = (ImageView) view.findViewById(R.id.channelImage);
            holder.chnumView = (TextView) view.findViewById(R.id.channelNumber);
            holder.onnowView = (TextView) view.findViewById(R.id.channelOnNow);
            holder.episodeView = (TextView) view.findViewById(R.id.channelEpisode);
            holder.chnameView = (TextView) view.findViewById(R.id.channelName);
            holder.updatedOn = (TextView) view.findViewById(R.id.updatedOn);
            view.setTag(holder);
        } else {
            holder = (MyViewHolder) view.getTag();
        }

        // Adding data to the Views/holder
        Glide.with(getContext()).load(epgitem.imgsrc).into(holder.imageView);
     //   MainActivity.loadOnNowView(position,epgitem.offset,epgitem.cssid);
        holder.chnumView.setText(miniGuideList.get(position).chnum);
        holder.onnowView.setText(miniGuideList.get(position).onnow);
        holder.chnameView.setText(miniGuideList.get(position).name);
        holder.episodeView.setText(miniGuideList.get(position).episode);
        holder.updatedOn.setText(getTime());
        return view;
    }

    public String getTime() {
        // Get the current time
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm: a", Locale.getDefault());
        return dateFormatter.format(calendar.getTime());
    }
    public static class MyViewHolder {
        public ImageView imageView;
        public TextView chnumView;
        public TextView onnowView;
        public TextView chnameView;
        public TextView episodeView;
        public TextView updatedOn;
        // ... and so on
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint,FilterResults results) {
            //    Log.d("SPECDEBUG", "in publishResults");
                miniGuideList = (ArrayList <EpgMapData> ) results.values; // has the filtered values
                Log.d("SPECDEBUG", "in publishResults minGuideList size = " + miniGuideList.size());
                notifyDataSetChanged();  // notifies the data with new filtered values
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();        // Holds the results of a filtering operation in values
                ArrayList <EpgMapData> FilteredArrList = new ArrayList<EpgMapData>();

                if (mOriginalValues == null) {
                    mOriginalValues = new ArrayList<EpgMapData>(miniGuideList); // saves the original data in mOriginalValues
                }

                /********
                 *
                 *  If constraint(CharSequence that is received) is null returns the mOriginalValues(Original) values
                 *  else does the Filtering and returns FilteredArrList(Filtered)
                 *
                 ********/
                if (constraint == null || constraint.length() == 0) {
                    // set the Original result to return
                    results.count = mOriginalValues.size();
                    results.values = mOriginalValues;
                } else {
                    // do the filter
                    String myconstraint = constraint.toString();
                    Log.d("SPECDEBUG", " Filter Contrants = " + myconstraint);
                    for (int i = 0; i < mOriginalValues.size(); i++) {
                        String data = " " + mOriginalValues.get(i).chnum + " ";
                    //    if (data.toLowerCase().startsWith(constraint.toString())) {
                          if (myconstraint.contains(data)) {
                              Log.d("SPECDEBUG", " Filter Contrants adding chnum = " + data);
                            FilteredArrList.add(mOriginalValues.get(i));
                        }
                    }
                    // set the Filtered result to return
                    results.count = FilteredArrList.size();
                    results.values = FilteredArrList;
                }
                return results;
            }
        };
        return filter;
    }

    public ArrayList<EpgMapData> getMiniGuideList() {
        return miniGuideList;
    }

    public int getBestChannelPosition(String chnum) {
        int pos = miniGuideList.size() - 1; // if not found goto end of list
        int chint = Integer.parseInt(chnum);
        for ( int i = 0; i < miniGuideList.size(); i++) {
            if ( Integer.parseInt(miniGuideList.get(i).chnum) == chint) {
                Log.d("SPECDEBUG", " getBestChannelPosition() chnum = " + chnum + "pos = " + i);
                pos = i;
                break;
            }
            if (i+1 < miniGuideList.size() && Integer.parseInt(miniGuideList.get(i+1).chnum) > chint )
            {
                // assume list is sorted, return the current position as best match
                Log.d("SPECDEBUG", " getBestChannelPosition() best pos chnum = " + chnum + "pos = " + i);
                pos = i;
                break;
            }
        }
        return pos;
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (listener != null) {
            listener.onDataSetChanged();
        }
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged();
    }
}


