package com.destiny.sta.ui;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.destiny.sta.ui.fragment.LoadingDialogFragment;

/**
 * Created by Bobo on 8/8/2017.
 */

public abstract class FragmentBase extends Fragment {

    public void showLoadingDialog() {
        DialogFragment loadingDialog = (DialogFragment) getActivity()
                .getSupportFragmentManager()
                .findFragmentByTag(LoadingDialogFragment.TAG);

        if(loadingDialog == null) {
            loadingDialog = new LoadingDialogFragment();
            loadingDialog.setCancelable(false);
            loadingDialog.show(getActivity().getSupportFragmentManager(), LoadingDialogFragment.TAG);
        }
    }

    public void dismissLoadingDialog() {
        DialogFragment loadingDialog = (DialogFragment) getActivity()
                .getSupportFragmentManager()
                .findFragmentByTag(LoadingDialogFragment.TAG);

        if(loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

}
