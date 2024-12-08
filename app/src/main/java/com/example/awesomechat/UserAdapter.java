package com.example.awesomechat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private ArrayList<User> users;
    private OnUserClickListener listener;
    private Context context;

    public interface OnUserClickListener {
        void onUserClick(int position);
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public UserAdapter(ArrayList<User> users) {
        this.users = users;
    }
    public UserAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        UserViewHolder viewHolder = new UserViewHolder(view, listener);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);

        holder.userNameTextView.setText(user.getName());

        // Load avatar image
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(holder.avatarImageView.getContext())
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.baseline_person_24)
                    .into(holder.avatarImageView);
        } else {
            holder.avatarImageView.setImageResource(R.drawable.baseline_person_24);
        }
    }

    @Override
    public int getItemCount() {

        return users.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {

        public ImageView avatarImageView;
        public TextView userNameTextView;

        public UserViewHolder(@NonNull View itemView, OnUserClickListener listener) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            listener.onUserClick(position);
                        }
                    }
                }
            });
        }
    }
}
