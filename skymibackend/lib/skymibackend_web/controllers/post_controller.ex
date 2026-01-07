defmodule SkymibackendWeb.PostController do
  use SkymibackendWeb, :controller

  import Ecto.Query, only: [from: 2]

  alias Skymibackend.Models.Post
  alias Skymibackend.Models.Images
  alias Skymibackend.Repo
  alias Skymibackend.JWT

  def create(conn, %{"post_title" => title,"post_description" => description,"post_date" => date,
    "bortle_scale" => bortle_scale,"moon_phase" => moon_phase,"image_data" => image_data}) do
    authheader = conn |> get_req_header("authorization") |> hd() |> String.split() |> Enum.at(1)
    case JWT.verify_and_validate(authheader, JWT.signer) do
      {:ok, data} ->
        {_0, iso_datetime, _1} = DateTime.from_iso8601(date)
        changeset = %Post{} |> Post.changeset(%{
          "post_title" => title,
          "post_description" => description,
          "post_date" => iso_datetime,
          "bortle_scale" => String.to_integer(bortle_scale),
          "moon_phase" => moon_phase
        }) |> Ecto.Changeset.put_change(:user_id, data["sup"])
        IO.inspect(changeset)
        case Repo.insert(changeset) do
          {:ok, post} ->
            IO.puts("data is put!")
            try do 
              Skymibackend.handle_userposts(post.postid, image_data) |> Enum.each(fn {:ok, file} ->
                IO.puts(file)
                image_changeset = Images.changeset(%Images{}, %{
                  "post_id" => post.postid,
                  "image_path" => file
                })
                case Repo.insert(image_changeset) do
                  {:ok, _img} -> :ok
                  {:error, _errmsg} -> throw(file)
                end
              end)
            catch file ->
              conn |> put_status(:internal_server_error) |> json(%{message: "failed to process #{file}"})
            end
            conn |> put_status(:ok) |> json(%{message: "Success"})
          {:error, _} ->
            IO.puts("data is not put!")
            conn |> put_status(:bad_request) |> json(%{
              message: "oops! looks like this user doesn't exist!"
            })
        end
      {:error, err} ->
        if Keyword.keyword?(err) do
          if err |> Access.get(:exp) == "token expired" do
            conn |> put_status(:unauthorized) |> json(%{message: "Token expired"})
          end
        end
        conn |> put_status(:not_found)
    end
  end

  def getposts(conn, _params) do
    authheader = conn |> get_req_header("authorization") |> hd() |> String.split() |> Enum.at(1)
    case JWT.verify_and_validate(authheader, JWT.signer) do
      {:ok, data} ->
        dbquery = from(post in Post,
        join: img in Images,
        on: post.postid == img.post_id,
        where: post.user_id == ^data["sup"],
        group_by: [
          post.postid, post.post_title,post.post_description,post.post_date,
          post.bortle_scale, post.moon_phase
        ],
        select: {
          post.postid,post.post_title,
          post.post_description,post.post_date,
          post.bortle_scale, post.moon_phase, fragment("array_agg(?)", img.image_path)
        })
        queryout = Repo.all(dbquery)
        conn |> put_status(:ok) |> json(%{data: queryout})
    end
  end
end
