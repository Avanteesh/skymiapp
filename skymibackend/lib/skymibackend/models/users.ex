defmodule Skymibackend.Models.Users do
  use Ecto.Schema
  import Ecto.Changeset

  @primary_key {:userid, Ecto.ULID, autogenerate: true}

  schema "users" do
    field :username, :string
    field :email, :string
    field :actual_password, :string, virtual: true, redact: true
    field :password, :binary, redact: true
    field :profile_picture, :string
    field :authprovider, :string
    timestamps()
  end

  def changeset(user, params \\ %{}) do
    user
     |> cast(params, [:username, :email, :actual_password, :profile_picture, :authprovider])
     |> validate_required([:username, :email, :authprovider])
     |> validate_length(:password, min: 8)
     |> put_password_hash()
     |> unique_constraint(:username)
     |> validate_format(:email, ~r/@/)
     |> validate_inclusion(:authprovider, ["google","local"])
  end

  defp put_password_hash(changeset) do
    case get_change(changeset, :actual_password) do
      nil -> changeset
      password -> put_change(changeset, :password, Argon2.hash_pwd_salt(password))
    end
  end
end

