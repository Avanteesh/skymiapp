defmodule SkymibackendWeb.Router do
  use SkymibackendWeb, :router

  pipeline :browser do
    plug :accepts, ["html"]
    plug :fetch_session
    plug :fetch_live_flash
    plug :put_root_layout, html: {SkymibackendWeb.Layouts, :root}
    plug :protect_from_forgery
    plug :put_secure_browser_headers
  end

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/api", SkymibackendWeb do
    pipe_through :api
    
    post "/signin", UserController, :create
    post "/login", UserController, :login
    post "/newpost", PostController, :create
    get "/getuserposts", PostController, :getposts
    get "/user/me", UserController, :getuserdata
    get "/profile-picture/:name", FileController, :userprofileloader
  end

  # Other scopes may use custom stacks.
  # scope "/api", SkymibackendWeb do
  #   pipe_through :api
  # end

  # Enable LiveDashboard and Swoosh mailbox preview in development
  if Application.compile_env(:skymibackend, :dev_routes) do
    # If you want to use the LiveDashboard in production, you should put
    # it behind authentication and allow only admins to access it.
    # If your application does not have an admins-only section yet,
    # you can use Plug.BasicAuth to set up some basic authentication
    # as long as you are also using SSL (which you should anyway).
    import Phoenix.LiveDashboard.Router

    scope "/dev" do
      pipe_through :browser

      live_dashboard "/dashboard", metrics: SkymibackendWeb.Telemetry
      forward "/mailbox", Plug.Swoosh.MailboxPreview
    end
  end
end
