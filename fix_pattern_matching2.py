#!/usr/bin/env python3
"""
Script to rewrite Java pattern matching for instanceof to traditional syntax using javalang parser.
"""

import sys
import os
from pathlib import Path

try:
    import javalang
except ImportError:
    print("javalang not installed. Run: pip install javalang")
    sys.exit(1)

def transform_pattern_matching(node):
    """
    Recursively transform pattern matching instanceof in the AST.
    Returns True if any transformation was made.
    """
    transformed = False
    
    if isinstance(node, javalang.tree.IfStatement):
        # Check if condition is InstanceOf with pattern variable
        if isinstance(node.condition, javalang.tree.InstanceOf):
            pattern = node.condition.pattern
            if pattern and hasattr(pattern, 'name') and pattern.name:
                # This is pattern matching: expr instanceof Type var
                # Transform to: expr instanceof Type
                # And add variable declaration at start of then_statement
                expr = node.condition.expression
                type_name = pattern.type.name if hasattr(pattern.type, 'name') else str(pattern.type)
                var_name = pattern.name
                
                # Modify condition to remove pattern
                node.condition.pattern = None
                
                # Add variable declaration to then_statement
                if isinstance(node.then_statement, javalang.tree.BlockStatement):
                    # Create variable declaration: Type var = (Type) expr;
                    var_decl = javalang.tree.LocalVariableDeclaration(
                        modifiers=[],
                        type=javalang.tree.ReferenceType(
                            name=type_name,
                            arguments=[]
                        ),
                        declarators=[
                            javalang.tree.VariableDeclarator(
                                name=var_name,
                                initializer=javalang.tree.Cast(
                                    type=javalang.tree.ReferenceType(name=type_name, arguments=[]),
                                    expression=expr
                                )
                            )
                        ]
                    )
                    # Insert at beginning of block
                    node.then_statement.statements.insert(0, var_decl)
                    transformed = True
    
    elif isinstance(node, javalang.tree.TernaryExpression):
        # Check if condition is InstanceOf with pattern variable
        if isinstance(node.condition, javalang.tree.InstanceOf):
            pattern = node.condition.pattern
            if pattern and hasattr(pattern, 'name') and pattern.name:
                # This is pattern matching in ternary: expr instanceof Type var ? var : false_expr
                # Transform to: expr instanceof Type ? (Type) expr : false_expr
                expr = node.condition.expression
                type_name = pattern.type.name if hasattr(pattern.type, 'name') else str(pattern.type)
                
                # Modify condition to remove pattern
                node.condition.pattern = None
                
                # Replace then_expression (which should be the pattern variable) with cast expression
                if isinstance(node.then_expression, javalang.tree.MemberReference) and node.then_expression.member == pattern.name:
                    node.then_expression = javalang.tree.Cast(
                        type=javalang.tree.ReferenceType(name=type_name, arguments=[]),
                        expression=expr
                    )
                    transformed = True
    
    elif isinstance(node, javalang.tree.WhileStatement):
        # Check if condition is InstanceOf with pattern variable
        if isinstance(node.condition, javalang.tree.InstanceOf):
            pattern = node.condition.pattern
            if pattern and hasattr(pattern, 'name') and pattern.name:
                expr = node.condition.expression
                type_name = pattern.type.name if hasattr(pattern.type, 'name') else str(pattern.type)
                var_name = pattern.name
                
                node.condition.pattern = None
                
                if isinstance(node.body, javalang.tree.BlockStatement):
                    var_decl = javalang.tree.LocalVariableDeclaration(
                        modifiers=[],
                        type=javalang.tree.ReferenceType(name=type_name, arguments=[]),
                        declarators=[
                            javalang.tree.VariableDeclarator(
                                name=var_name,
                                initializer=javalang.tree.Cast(
                                    type=javalang.tree.ReferenceType(name=type_name, arguments=[]),
                                    expression=expr
                                )
                            )
                        ]
                    )
                    node.body.statements.insert(0, var_decl)
                    transformed = True
    
    # Recurse into children
    for attr_name in dir(node):
        if attr_name.startswith('_'):
            continue
        attr = getattr(node, attr_name)
        if isinstance(attr, javalang.tree.Node):
            if transform_pattern_matching(attr):
                transformed = True
        elif isinstance(attr, list):
            for item in attr:
                if isinstance(item, javalang.tree.Node):
                    if transform_pattern_matching(item):
                        transformed = True
    
    return transformed

def fix_file(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return False
    
    try:
        # Parse the Java file
        tree = javalang.parse.parse(content)
    except javalang.parser.JavaSyntaxError as e:
        print(f"Syntax error in {filepath}: {e}")
        return False
    except Exception as e:
        print(f"Parse error in {filepath}: {e}")
        return False
    
    # Transform the AST
    transformed = transform_pattern_matching(tree)
    
    if not transformed:
        return False
    
    # Convert back to source code
    # javalang doesn't have a built-in pretty printer, so we need to use a different approach
    # For now, let's use the tokenizer approach
    try:
        tokens = list(javalang.tokenizer.tokenize(content))
        # This is complex - we need to modify tokens based on AST changes
        # Instead, let's use a simpler approach: just use the regex but more carefully
        pass
    except Exception as e:
        print(f"Tokenize error in {filepath}: {e}")
        return False
    
    return False

def main():
    java_files = list(Path('android/app/src/main/java').rglob('*.java'))
    fixed_count = 0
    for filepath in java_files:
        if fix_file(filepath):
            fixed_count += 1
            print(f"Fixed: {filepath}")
    print(f"Total files fixed: {fixed_count}")

if __name__ == '__main__':
    main()