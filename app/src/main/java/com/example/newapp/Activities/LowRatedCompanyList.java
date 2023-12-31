package com.example.newapp.Activities;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newapp.Adapter.CompanyAdapter;
import com.example.newapp.DataModel.Company;
import com.example.newapp.DataModel.SpaceShip;
import com.example.newapp.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class LowRatedCompanyList extends Fragment {

    private ArrayList<Company> companyArrayList;
    private SearchView searchCompany;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private CompanyAdapter companyAdapter;
    private String loginMode;
    CompanyAdapter.OnCompanyClickListener onCompanyClickListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView no_rated_tv;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_low_rated_company_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FirebaseApp.initializeApp(getActivity());

        searchCompany = getView().findViewById(R.id.srchCompany_lowRated);

        companyArrayList = new ArrayList<>();

        progressBar = getView().findViewById(R.id.progressbar_lowRated);
        recyclerView = getView().findViewById(R.id.recycler_lowRated);
        swipeRefreshLayout = getView().findViewById(R.id.swip_ref_low_company_list);
        no_rated_tv = getView().findViewById(R.id.no_rated_tv);

        Intent intent1 = getActivity().getIntent();
        loginMode = intent1.getStringExtra("loginMode");

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getLowRatedCompanies(searchCompany.getQuery().toString());
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        getLowRatedCompanies(searchCompany.getQuery().toString());


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
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                getLowRatedCompanies(newText);
                return false;
            }
        });

    }

    // fetch the low rated companies and set them to adapter based upon query in searchView
    private void getLowRatedCompanies(String userQuery) {
        companyArrayList.clear();
        try {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("company");
            Query query = databaseReference;
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    companyArrayList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Company company = dataSnapshot.getValue(Company.class);
                        ArrayList<SpaceShip> spaceShipArrayList = company.getSpaceShips();
                        boolean isLowRated = false;
                        if (spaceShipArrayList != null) {
                            for (SpaceShip spaceShip : spaceShipArrayList) {
                                Float shipRating = Float.parseFloat(spaceShip.getSpaceShipRating());
                                if (shipRating <= 1 && shipRating > 0) {
                                    isLowRated = true;
                                }
                            }
                        }
                        if (isLowRated && company.getName().toLowerCase().contains(userQuery.toLowerCase())) {
                            companyArrayList.add(dataSnapshot.getValue(Company.class));
                        }

                    }
                    setAdapter(companyArrayList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Slow Internet Connection", Toast.LENGTH_SHORT).show();
        }
    }


    // Setting up the adapter to show the list of companies in the arraylist.
    private void setAdapter(ArrayList<Company> arrayList) {
        progressBar.setVisibility(View.GONE);
        if(arrayList.size()==0){
            no_rated_tv.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        else{
            no_rated_tv.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        companyAdapter = new CompanyAdapter(arrayList, getActivity(), onCompanyClickListener);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(companyAdapter);
        companyAdapter.notifyDataSetChanged();

    }

}