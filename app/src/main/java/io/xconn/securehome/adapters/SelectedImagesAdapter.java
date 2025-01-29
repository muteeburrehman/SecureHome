package io.xconn.securehome.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import io.xconn.securehome.R;
import java.util.ArrayList;
import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder> {
    private final List<Uri> imageUris;
    private final Context context;
    private final OnImageRemoveListener listener;
    private final RequestOptions glideOptions;

    public interface OnImageRemoveListener {
        void onImageRemove(int position);
    }

    public SelectedImagesAdapter(Context context, OnImageRemoveListener listener) {
        this.context = context;
        this.imageUris = new ArrayList<>();
        this.listener = listener;
        setHasStableIds(true); // Enable stable IDs for better recycling

        // Configure Glide options once for reuse
        this.glideOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.baseline_image_24) // Add a placeholder drawable
                .error(R.drawable.remove); // Add an error drawable
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);

        // Clear any previous image loading request
        Glide.with(context).clear(holder.imageView);

        // Load new image with Glide
        Glide.with(context)
                .load(imageUri)
                .apply(glideOptions)
                .into(holder.imageView);

        // Use view tags to prevent wrong clicks during recycling
        holder.removeButton.setTag(position);
        holder.removeButton.setOnClickListener(v -> {
            int pos = (int) v.getTag();
            if (listener != null && pos >= 0 && pos < imageUris.size()) {
                listener.onImageRemove(pos);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    // Efficient batch addition of images
    public void addImages(List<Uri> uris) {
        if (uris != null && !uris.isEmpty()) {
            int startPosition = imageUris.size();
            imageUris.addAll(uris);
            notifyItemRangeInserted(startPosition, uris.size());
        }
    }

    // Efficient removal with proper position checking
    public void removeImage(int position) {
        if (position >= 0 && position < imageUris.size()) {
            imageUris.remove(position);
            notifyItemRemoved(position);
            // Only notify about changed range if there are items after the removed position
            if (position < imageUris.size()) {
                notifyItemRangeChanged(position, imageUris.size() - position);
            }
        }
    }

    // Return a defensive copy of the list
    public List<Uri> getImageUris() {
        return new ArrayList<>(imageUris);
    }

    // Improved ViewHolder with field initialization
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final ImageView removeButton;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.selected_image);
            removeButton = itemView.findViewById(R.id.btn_remove_image);

            // Pre-set layout parameters for better performance
            itemView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }
    }
}