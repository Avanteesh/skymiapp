defmodule Skymibackend.Repo.Migrations.CreatePost do
  use Ecto.Migration

  def change do
    create table(:post, primary_key: false) do
      add :postid, :binary_id, primary_key: true
      add :user_id, references(:users, column: :userid, type: :binary_id, on_delete: :delete_all), null: false
      add :post_title, :string, null: false
      add :post_description, :string
      add :post_date, :utc_datetime, null: false
      add :bortle_scale, :integer
      add :moon_phase, :string
      timestamps()
    end
  end
end
