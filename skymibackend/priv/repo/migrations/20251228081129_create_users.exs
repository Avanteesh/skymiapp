defmodule Skymibackend.Repo.Migrations.CreateUsers do
  use Ecto.Migration

  def change do
    create table(:users, primary_key: false) do
      add :userid, :binary_id, primary_key: true
      add :username, :string, null: false
      add :email, :string, null: false
      add :password, :binary, null: false
      add :profile_picture, :string
      add :authprovider, :string
      timestamps()
    end

    create unique_index(:users, [:email])
    create unique_index(:users, [:username])
  end
end
