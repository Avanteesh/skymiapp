defmodule Skymibackend.Repo.Migrations.CreateImage do
  use Ecto.Migration

  def change do
    create table(:image, primary_key: false) do
      add :image_id, :binary_id, primary_key: true
      add :post_id, references(:post, column: :postid, type: :binary_id, on_delete: :delete_all), null: false
      add :image_path, :string, null: false
    end
    create unique_index(:image, [:post_id, :image_path])
  end
end
