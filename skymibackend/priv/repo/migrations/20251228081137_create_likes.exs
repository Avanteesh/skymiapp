defmodule Skymibackend.Repo.Migrations.CreateLike do
  use Ecto.Migration

  def change do
    create table(:like, primary_key: false) do
      add :likeid, :binary_id, primary_key: true
      add :post_id, references(:post, column: :postid, type: :binary_id, on_delete: :delete_all), null: false
      add :liked_user_id, references(:users, column: :userid, type: :binary_id, on_delete: :delete_all), null: false
      timestamps()
    end
    create unique_index(:like, [:post_id, :liked_user_id])
  end
end
