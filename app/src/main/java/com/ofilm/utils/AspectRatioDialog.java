package com.ofilm.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ofilm.camera.R;

import java.util.Arrays;
import java.util.Set;

public class AspectRatioDialog extends DialogFragment {

    private static final String ARG_ASPECT_RATIOS = "aspect_ratio";
    private static final String ARG_CURRENT_ASPECT_RATIO = "current_aspect_ratio";

    private Listener mListener;

    public static AspectRatioDialog newInstance(Set<AspectRatio> ratios, AspectRatio currentRatio){
        final AspectRatioDialog fragment = new AspectRatioDialog();
        final Bundle args = new Bundle();

        args.putParcelableArray(ARG_ASPECT_RATIOS,
                ratios.toArray(new AspectRatio[ratios.size()]));
        args.putParcelable(ARG_CURRENT_ASPECT_RATIO, currentRatio);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (Listener) context;
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Bundle args = getArguments();
        final AspectRatio[] ratios = (AspectRatio[]) args.getParcelableArray(ARG_ASPECT_RATIOS);
        if(ratios == null){
            throw new RuntimeException("no ratios");
        }
        Arrays.sort(ratios);

        final AspectRatio current = args.getParcelable(ARG_CURRENT_ASPECT_RATIO);
        final AspectRatioAdapter adapter = new AspectRatioAdapter(ratios, current);

        return new AlertDialog.Builder(getActivity())
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onAspectRatioSelected(ratios[which]);
                    }
                }).create();
    }


    private static class AspectRatioAdapter extends BaseAdapter{

        private final AspectRatio[] mRatios;
        private final AspectRatio mCurrentRatio;

        AspectRatioAdapter(AspectRatio[] ratios, AspectRatio current){
            mRatios = ratios;
            mCurrentRatio = current;
        }


        @Override
        public int getCount() {
            return mRatios.length;
        }

        @Override
        public AspectRatio getItem(int position) {
            return mRatios[position];
        }

        @Override
        public long getItemId(int position) {
            return mRatios[position].hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AspectRatioAdapter.ViewHolder holder;
            AspectRatio ratio = getItem(position);
            StringBuilder sb = new StringBuilder(ratio.toString());
            if(convertView == null){
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.aspect_dialog_item,
                        parent, false);
                holder = new AspectRatioAdapter.ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.text1);
                convertView.setTag(holder);
            }else{
                holder = (AspectRatioAdapter.ViewHolder) convertView.getTag();
            }
            if(ratio.equals(mCurrentRatio)){
                sb.append(" *");
            }
            if(ratio == AspectRatio.of(4,3) || ratio == AspectRatio.of(16,9)) {
                holder.text.setText(sb); // 显示
            }else{
                convertView.setVisibility(View.GONE);
            }
            return convertView;
        }

        private static class ViewHolder {
            TextView text;
        }
    }

    public interface Listener {
        void onAspectRatioSelected(@NonNull AspectRatio ratio);
    }
}
