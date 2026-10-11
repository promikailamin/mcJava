#!/usr/bin/env python3
"""
Proper pattern matching rewriter using javalang parser with position tracking.
"""

import sys
import os
from pathlib import Path

try:
    import javalang
except ImportError:
    print("javalang not installed. Run: pip install javalang")
    sys.exit(1)

class PatternMatchingRewriter:
    def __init__(self, content):
        self.content = content
        self.replacements = []  # List of (start, end, replacement_text)
    
    def add_replacement(self, start, end, new_text):
        """Add a replacement to be applied later."""
        self.replacements.append((start, end, new_text))
    
    def apply_replacements(self):
        """Apply all replacements in reverse order to not affect positions."""
        # Sort by start position descending
        self.replacements.sort(key=lambda x: x[0], reverse=True)
        result = self.content
        for start, end, new_text in self.replacements:
            result = result[:start] + new_text + result[end:]
        return result

    def get_text(self, node):
        """Get source text for a node using position info."""
        if not hasattr(node, 'position') or not node.position:
            return None
        line, col = node.position
        # This is approximate - we need to find the actual text
        lines = self.content.split('\n')
        if line <= len(lines):
            line_text = lines[line - 1]
            # Find the column (1-indexed)
            if col <= len(line_text) + 1:
                return line_text[col - 1:]
        return None

    def visit(self, node, parent=None):
        """Visit AST nodes and collect pattern matching transformations."""
        if node is None:
            return
        
        # Handle InstanceOf with pattern variable
        if isinstance(node, javalang.tree.InstanceOf):
            if node.pattern and hasattr(node.pattern, 'name') and node.pattern.name:
                self.transform_instanceof(node, parent)
        
        # Recurse
        for attr_name in dir(node):
            if attr_name.startswith('_'):
                continue
            attr = getattr(node, attr_name)
            if isinstance(attr, javalang.tree.Node):
                self.visit(attr, node)
            elif isinstance(attr, list):
                for item in attr:
                    if isinstance(item, javalang.tree.Node):
                        self.visit(item, node)
    
    def transform_instanceof(self, node, parent):
        """Transform pattern matching instanceof."""
        expr = node.expression
        pattern = node.pattern
        type_name = self.get_type_name(pattern.type)
        var_name = pattern.name
        
        # Get the source text for the expression
        expr_text = self.get_node_text(expr)
        if not expr_text:
            return
        
        # Get the full instanceof expression text
        instanceof_text = self.get_node_text(node)
        if not instanceof_text:
            return
        
        # Determine the context and generate appropriate replacement
        if isinstance(parent, javalang.tree.TernaryExpression):
            # Ternary: expr instanceof Type var ? true_expr : false_expr
            self.transform_ternary_instanceof(node, parent, expr_text, type_name, var_name)
        elif isinstance(parent, javalang.tree.IfStatement):
            # If statement condition
            self.transform_if_instanceof(node, parent, expr_text, type_name, var_name)
        elif isinstance(parent, javalang.tree.WhileStatement):
            # While statement condition
            self.transform_while_instanceof(node, parent, expr_text, type_name, var_name)
        elif isinstance(parent, javalang.tree.BinaryOperation):
            # Complex condition with && or ||
            self.transform_binary_instanceof(node, parent, expr_text, type_name, var_name)
        elif isinstance(parent, javalang.tree.VariableDeclarator):
            # Variable assignment
            self.transform_variable_instanceof(node, parent, expr_text, type_name, var_name)
        elif isinstance(parent, javalang.tree.ReturnStatement):
            # Return statement
            self.transform_return_instanceof(node, parent, expr_text, type_name, var_name)
        else:
            # Default: just remove pattern variable
            self.transform_simple_instanceof(node, expr_text, type_name, var_name)
    
    def get_type_name(self, type_node):
        """Get type name from type node."""
        if hasattr(type_node, 'name'):
            return type_node.name
        return str(type_node)
    
    def get_node_text(self, node):
        """Get source text for a node."""
        if not hasattr(node, 'position') or not node.position:
            return None
        line, col = node.position
        lines = self.content.split('\n')
        if line <= len(lines):
            # This is a rough approximation - we need the end position too
            return lines[line - 1][col - 1:]
        return None
    
    def get_exact_text(self, start_line, start_col, end_line, end_col):
        """Get exact text between two positions."""
        lines = self.content.split('\n')
        if start_line == end_line:
            return lines[start_line - 1][start_col - 1:end_col - 1]
        else:
            parts = []
            parts.append(lines[start_line - 1][start_col - 1:])
            for l in range(start_line, end_line - 1):
                parts.append(lines[l])
            parts.append(lines[end_line - 1][:end_col - 1])
            return '\n'.join(parts)
    
    def transform_simple_instanceof(self, node, expr_text, type_name, var_name):
        """Simple instanceof: just remove pattern."""
        # Find the exact instanceof expression in source
        # This is complex without exact positions
        pass
    
    def transform_ternary_instanceof(self, node, parent, expr_text, type_name, var_name):
        """Transform ternary with pattern matching."""
        # Check if true expression is just the variable
        if isinstance(parent.then_expression, javalang.tree.MemberReference):
            if parent.then_expression.member == var_name:
                # Simple case: expr instanceof Type var ? var : false_expr
                # Replace with: expr instanceof Type ? (Type) expr : false_expr
                false_text = self.get_node_text(parent.else_expression)
                if false_text:
                    # We need to find the exact text to replace
                    pass
    
    def transform_if_instanceof(self, node, parent, expr_text, type_name, var_name):
        """Transform if statement with pattern matching."""
        # Need to add variable declaration in then block
        pass
    
    def transform_while_instanceof(self, node, parent, expr_text, type_name, var_name):
        """Transform while statement with pattern matching."""
        pass
    
    def transform_binary_instanceof(self, node, parent, expr_text, type_name, var_name):
        """Transform binary operation with pattern matching."""
        pass
    
    def transform_variable_instanceof(self, node, parent, expr_text, type_name, var_name):
        """Transform variable declaration with pattern matching."""
        pass
    
    def transform_return_instanceof(self, node, parent, expr_text, type_name, var_name):
        """Transform return statement with pattern matching."""
        pass

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    try:
        tree = javalang.parse.parse(content)
    except Exception as e:
        print(f"Parse error in {filepath}: {e}")
        return False
    
    rewriter = PatternMatchingRewriter(content)
    rewriter.visit(tree)
    
    new_content = rewriter.apply_replacements()
    
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    fixed_count = 0
    for filepath in java_files:
        try:
            if process_file(filepath):
                fixed_count += 1
                print(f"Fixed: {filepath}")
        except Exception as e:
            print(f"Error processing {filepath}: {e}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()