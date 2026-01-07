defmodule Skymibackend do
  @moduledoc """
  Skymibackend keeps the contexts that define your domain
  and business logic.

  Contexts are also responsible for managing your data, regardless
  if it comes from the database, an external API or others.
  """
  def handle_useravatars(user_id, uploads) do
    priv_path = :code.priv_dir(:skymibackend)
    if not File.exists?(Path.join([priv_path,"static","uploads","profile_pictures"])) do
      File.mkdir(Path.join([priv_path,"static","uploads","profile_pictures"]))
    end
    profile_picfolder = Path.join([priv_path,"static","uploads","profile_pictures"])
    ext = Path.extname(uploads.filename)
    newfile_name = "user_#{user_id}#{ext}"
    file_dest = Path.join(profile_picfolder, newfile_name)
    File.cp!(uploads.path, file_dest) 
    {:ok, newfile_name}
  end

  def handle_userposts(post_id, uploads)  do
    priv_path = :code.priv_dir(:skymibackend)
    if not File.exists?(Path.join([priv_path,"static","uploads","post-images"])) do
      File.mkdir(Path.join([priv_path, "static", "uploads","post-images"]))
    end
    posts_path = Path.join([priv_path, "static", "uploads", "post-images", post_id])
    File.mkdir(posts_path)
    uploads |> Enum.with_index() |> Enum.map(fn {upload, idx} ->
      extension = Path.extname(upload.filename)
      newfile_name = "post_#{idx}#{extension}"
      file_dest = Path.join(posts_path, newfile_name)
      File.cp!(upload.path, file_dest)
      {:ok, Path.join([post_id,newfile_name])}
    end)
  end
end
