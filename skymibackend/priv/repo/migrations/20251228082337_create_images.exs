defmodule Skymibackend.Repo.Migrations.CreateImages do
  use Ecto.Migration

  def change do
    create table(:images, primary_key: false) do
      add :imageid, :binary_id, primary_key: true
      add :post_id, references(:post, column: :postid, type: :binary_id, on_delete: :delete_all), null: false
      add :image_path, :string, null: false
    end
    create unique_index(:images, [:post_id, :image_path])
  end
end
