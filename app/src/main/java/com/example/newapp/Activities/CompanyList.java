package com.example.newapp.Activities;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.newapp.Adapter.CompanyAdapter;
import com.example.newapp.DataModel.Company;
import com.example.newapp.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class CompanyList extends Fragment {

    private ArrayList<Company> companyArrayList;
    private SearchView searchCompany;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private CompanyAdapter companyAdapter;
    private String loginMode;
    private SwipeRefreshLayout swipeRefreshLayout;

    CompanyAdapter.OnCompanyClickListener onCompanyClickListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_companies_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FirebaseApp.initializeApp(getActivity());

        searchCompany = getView().findViewById(R.id.srchCompany);

        companyArrayList = new ArrayList<>();

        swipeRefreshLayout = getView().findViewById(R.id.swip_ref_company_list);
        progressBar = getView().findViewById(R.id.progressbar);
        recyclerView = getView().findViewById(R.id.recycler);

        Intent intent1 = getActivity().getIntent();
        loginMode = intent1.getStringExtra("loginMode");

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getCompanies(searchCompany.getQuery().toString());
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        getCompanies(searchCompany.getQuery().toString());


        onCompanyClickListener = new CompanyAdapter.OnCompanyClickListener() {
            @Override
            public void onCompaniesClicked(int position) {
                Intent intent = new Intent(getActivity(), CompanyDetailsActivity.class);
                intent.putExtra("loginMode", loginMode);
                intent.putExtra("companyID", companyArrayList.get(position).getCompanyId());
                intent.putExtra("company_name", companyArrayList.get(position).getName());
                intent.putExtra("company_desc", companyArrayList.get(position).getDescription());
                intent.putExtra("company_img", companyArrayList.get(position).getImageUrl());
                intent.putExtra("company_license", companyArrayList.get(position).getLicenseUrl());
                intent.putExtra("isAuthorised", companyArrayList.get(position).getOperational());
                startActivity(intent);
            }
        };

        // SearchView to enable searching of companies by name.
        searchCompany.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // if query in searchView is submitted do nothing
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            // if query in searchView changes update the data in recyclerView.
            @Override
            public boolean onQueryTextChange(String newText) {
                getCompanies(newText);
                return false;
            }
        });

    }


    // fetch the companies and set them to adapter based upon query in searchView
    private void getCompanies(String userQuery) {
        companyArrayList.clear();
        try {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("company");
            Query query = databaseReference;
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    companyArrayList.clear();
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot companySnapShot : dataSnapshot.getChildren()) {
                            Company company = companySnapShot.getValue(Company.class);
                            if (company != null) {
                                if (company.getOperational() && company.getName().toLowerCase().contains(userQuery.toLowerCase())) {
                                    companyArrayList.add(company);
                                }
                            }
                        }
                    }
                    setAdapter(companyArrayList);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getActivity(), databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Slow Internet Connection", Toast.LENGTH_SHORT).show();
        }
    }


    // Setting up the adapter to show the list of companies in the arraylist.
    private void setAdapter(ArrayList<Company> arrayList) {
        companyAdapter = new CompanyAdapter(arrayList, getActivity(), onCompanyClickListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(companyAdapter);
        companyAdapter.notifyDataSetChanged();
    }

}
