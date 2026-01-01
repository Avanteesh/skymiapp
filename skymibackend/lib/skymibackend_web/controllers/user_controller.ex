defmodule SkymibackendWeb.UserController do
  use SkymibackendWeb, :controller
  
  alias Skymibackend.Models.Users
  alias Skymibackend.Repo
  alias Skymibackend.JWT
  
  import Ecto.Query

  def create(conn, %{"username" => username, "email" => email, "password" => password,
  "profile_picture" => profile_picture}) do
    changeset = Users.changeset(%Users{}, %{
      "username" => username,
      "email" => email,
      "authprovider" => "local",
      "actual_password" => password,
      "profile_picture" => ""
    })
    case Repo.insert(changeset) do
      {:ok, user} ->
        {:ok, userfile} = Skymibackend.handle_useravatars(user.userid, profile_picture)
        user
          |> Ecto.Changeset.change(%{profile_picture: userfile})
          |> Repo.update()
        conn |> put_status(:created) |> json(%{
          message: "created!"
        })
      {:error, _} ->
        conn |> put_status(:bad_request) |> json(%{message: "This user is taken!"})
    end
  end

  def login(conn, %{"username" => username, "password" => password}) do
    dbquery = Repo.all(from u in Users, where: u.username == ^username) 
    if Enum.empty?(dbquery) == false do
        user = hd(dbquery)
        case Argon2.verify_pass(password, user.password) do
          true ->
            claims = %{"sup" => user.userid, "username" => user.username}
            {:ok, jwt_token, _} = JWT.generate_and_sign(claims, JWT.signer)
            conn |> put_status(:ok) |> json(%{
              token: jwt_token
            })
          false ->
            conn |> put_status(:unauthorized) |> json(%{
              message: "Either username or password is in correct!"
            })
        end
    else
      conn |> put_status(:not_found) |> json(%{
        message: "This user doesn't exist!"
      })
    end
  end

  def getuserdata(conn, _params) do 
    authheader = conn |> get_req_header("authorization") |> hd() |> String.split() |> Enum.at(1)
    case JWT.verify_and_validate(authheader, JWT.signer) do
      {:ok, data} ->
        dbquery = Repo.all(from user in Users, where: user.userid == ^data["sup"], select: {
          user.userid, user.username, user.profile_picture
        })
        if not Enum.empty?(dbquery) do
          user = hd(dbquery)
          conn |> put_status(:ok) |> json(%{
            id: user |> elem(0), username: user |> elem(1), profilepic: user |> elem(2)
          })
        else
          conn |> put_status(:not_found) |> json(%{message: "Invalid token!"})
        end
      {:error, ehandle} -> IO.inspect(ehandle)
    end
  end
end
