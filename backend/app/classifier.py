import os
import io
import logging
import numpy as np
from PIL import Image
import tensorflow as tf
from typing import Tuple, Optional, List

logger = logging.getLogger("ecotionbuddy.classifier")

class TrashClassifier:
    """MobileNetV2-based trash classifier for waste categorization"""
    
    def __init__(self, model_path: str):
        self.model_path = model_path
        self.model = None
        self.class_names = [
            "cardboard", "glass", "metal", "paper", "plastic", "trash"
        ]  # Common waste categories - will be auto-detected from model
        self.input_size = (224, 224)  # MobileNetV2 standard input size
        
    def load_model(self) -> bool:
        """Load the TensorFlow SavedModel"""
        try:
            if not os.path.exists(self.model_path):
                logger.error(f"Model path does not exist: {self.model_path}")
                return False
                
            # Check for required SavedModel files
            pb_file = os.path.join(self.model_path, "saved_model.pb")
            variables_dir = os.path.join(self.model_path, "variables")
            
            if not os.path.exists(pb_file):
                logger.error(f"saved_model.pb not found in {self.model_path}")
                return False
                
            if not os.path.exists(variables_dir):
                logger.warning(f"variables/ directory not found in {self.model_path}")
                # Some models might not have variables, continue anyway
            
            self.model = tf.saved_model.load(self.model_path)
            logger.info(f"Model loaded successfully from {self.model_path}")
            
            # Get inference function
            self.infer = self.model.signatures["serving_default"]
            logger.info(f"Model input signature: {list(self.infer.structured_input_signature[1].keys())}")
            logger.info(f"Model output signature: {list(self.infer.structured_outputs.keys())}")
            
            return True
            
        except Exception as e:
            logger.exception(f"Failed to load model: {e}")
            return False
    
    def preprocess_image(self, image_bytes: bytes) -> Optional[tf.Tensor]:
        """Preprocess image for MobileNetV2 inference"""
        try:
            # Load image from bytes
            image = Image.open(io.BytesIO(image_bytes))
            
            # Convert to RGB if needed
            if image.mode != 'RGB':
                image = image.convert('RGB')
            
            # Resize to model input size
            image = image.resize(self.input_size, Image.Resampling.LANCZOS)
            
            # Convert to numpy array and normalize
            img_array = np.array(image, dtype=np.float32)
            
            # MobileNetV2 preprocessing: scale to [-1, 1]
            img_array = (img_array / 127.5) - 1.0
            
            # Add batch dimension
            img_array = np.expand_dims(img_array, axis=0)
            
            # Convert to TensorFlow tensor
            return tf.constant(img_array)
            
        except Exception as e:
            logger.exception(f"Image preprocessing failed: {e}")
            return None
    
    def predict(self, image_bytes: bytes) -> Tuple[str, float]:
        """
        Classify trash image and return label with confidence
        
        Returns:
            Tuple[str, float]: (predicted_class, confidence_score)
        """
        if self.model is None:
            logger.error("Model not loaded. Call load_model() first.")
            return "unknown", 0.0
        
        try:
            # Preprocess image
            processed_image = self.preprocess_image(image_bytes)
            if processed_image is None:
                return "unknown", 0.0
            
            # Run inference
            # Note: Input key might be different, common ones are:
            # "input_1", "inputs", "input", "serving_default_input_1"
            input_keys = list(self.infer.structured_input_signature[1].keys())
            input_key = input_keys[0] if input_keys else "input_1"
            
            predictions = self.infer(**{input_key: processed_image})
            
            # Get output tensor (usually the first/only output)
            output_keys = list(predictions.keys())
            output_key = output_keys[0] if output_keys else "predictions"
            output_tensor = predictions[output_key]
            
            # Convert to numpy and get probabilities
            probs = tf.nn.softmax(output_tensor).numpy()[0]
            
            # Get predicted class and confidence
            predicted_idx = np.argmax(probs)
            confidence = float(probs[predicted_idx])
            
            # Map to class name
            if predicted_idx < len(self.class_names):
                predicted_class = self.class_names[predicted_idx]
            else:
                predicted_class = f"class_{predicted_idx}"
            
            logger.info(f"Prediction: {predicted_class} (confidence: {confidence:.3f})")
            return predicted_class, confidence
            
        except Exception as e:
            logger.exception(f"Prediction failed: {e}")
            return "unknown", 0.0
    
    def get_class_names(self) -> List[str]:
        """Get list of class names"""
        return self.class_names.copy()
    
    def set_class_names(self, class_names: List[str]):
        """Update class names if different from default"""
        self.class_names = class_names
        logger.info(f"Updated class names: {self.class_names}")


# Global classifier instance
_classifier_instance: Optional[TrashClassifier] = None

def get_classifier() -> Optional[TrashClassifier]:
    """Get global classifier instance"""
    return _classifier_instance

def initialize_classifier(model_path: str) -> bool:
    """Initialize global classifier instance"""
    global _classifier_instance
    
    try:
        _classifier_instance = TrashClassifier(model_path)
        success = _classifier_instance.load_model()
        
        if not success:
            _classifier_instance = None
            return False
            
        logger.info("Trash classifier initialized successfully")
        return True
        
    except Exception as e:
        logger.exception(f"Failed to initialize classifier: {e}")
        _classifier_instance = None
        return False
