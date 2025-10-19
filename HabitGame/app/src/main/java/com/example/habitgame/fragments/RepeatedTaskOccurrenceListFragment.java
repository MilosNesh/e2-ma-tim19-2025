package com.example.habitgame.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habitgame.R;
import com.example.habitgame.adapters.RepeatedTaskOccurrenceItemAdapter;
import com.example.habitgame.model.RepeatedTaskOccurence;
import com.example.habitgame.repositories.RepeatedTaskOccurrenceRepository;
import com.example.habitgame.services.RepeatedTaskOccurrenceService;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.List;

public class RepeatedTaskOccurrenceListFragment extends Fragment implements RepeatedTaskOccurrenceItemAdapter.Listener {

    private RecyclerView rv;
    private ProgressBar progress;
    private RepeatedTaskOccurrenceItemAdapter adapter;
    private final RepeatedTaskOccurrenceService service = new RepeatedTaskOccurrenceService();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inf.inflate(R.layout.fragment_repeated_task_occurrence_list, container, false);
        rv = v.findViewById(R.id.rvOccurrences);
        progress = v.findViewById(R.id.progress);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RepeatedTaskOccurrenceItemAdapter(this);
        rv.setAdapter(adapter);

        getChildFragmentManager().setFragmentResultListener(
                "series_toggle",
                this,
                (requestKey, bundle) -> loadThisWeek()
        );

        loadThisWeek();
        return v;
    }

    private void loadThisWeek(){
        showLoading(true);
        String uid = FirebaseAuth.getInstance().getCurrentUser()!=null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            showLoading(false);
            Snackbar.make(rv, "Niste prijavljeni.", Snackbar.LENGTH_LONG).show();
            return;
        }

        long[] range = weekRange();
        RepeatedTaskOccurrenceRepository.getByUserInRange(uid, range[0], range[1])
                .addOnSuccessListener(this::bind)
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Snackbar.make(rv, "Greška: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                });
    }

    private void bind(List<RepeatedTaskOccurence> list){
        showLoading(false);
        adapter.submitList(list);
    }

    private void showLoading(boolean b){
        progress.setVisibility(b?View.VISIBLE:View.GONE);
        rv.setAlpha(b?0.3f:1f);
    }

    private long[] weekRange(){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,0);
        c.set(Calendar.MINUTE,0);
        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND,0);

        int dow = c.get(Calendar.DAY_OF_WEEK);
        int delta = (dow==Calendar.MONDAY)?0 : ((dow==Calendar.SUNDAY)?6 : (dow-Calendar.MONDAY));
        c.add(Calendar.DAY_OF_MONTH, -delta);
        long start = c.getTimeInMillis();

        Calendar e = (Calendar) c.clone();
        e.add(Calendar.DAY_OF_MONTH, 6);
        long end = e.getTimeInMillis();
        return new long[]{start, end};
    }

    @Override
    public void onOpen(RepeatedTaskOccurence occ) {
        onOpenDetails(occ);
    }

    @Override
    public void onDone(RepeatedTaskOccurence occ) {
        onMarkDone(occ);
    }

    @Override
    public void onCancel(RepeatedTaskOccurence occ) {
        service.markCanceled(occ)
                .addOnSuccessListener(v -> {
                    Snackbar.make(rv, "Pojava otkazana.", Snackbar.LENGTH_SHORT).show();
                    loadThisWeek();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rv, "Greška: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }

    @Override
    public void onMarkDone(RepeatedTaskOccurence occ) {
        service.markDone(occ)
                .addOnSuccessListener(v -> {
                    Snackbar.make(rv, "Dobio si +" + occ.getXp() + " XP", Snackbar.LENGTH_SHORT).show();
                    loadThisWeek();
                })
                .addOnFailureListener(e ->
                        Snackbar.make(rv, "Greška: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
    }

    @Override
    public void onOpenDetails(RepeatedTaskOccurence occ) {
        RepeatedTaskOccurrenceDetailsBottomSheet.newInstance(occ)
                .show(getChildFragmentManager(), "occ_details");
    }
}
