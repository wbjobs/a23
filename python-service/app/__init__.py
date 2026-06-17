from flask import Flask
from flask_cors import CORS
from config import Config


def create_app(config_class=Config):
    app = Flask(__name__)
    app.config.from_object(config_class)
    
    CORS(app, resources={r"/api/*": {"origins": "*"}})
    
    from app.routes.parse_routes import parse_bp
    app.register_blueprint(parse_bp)
    
    return app
