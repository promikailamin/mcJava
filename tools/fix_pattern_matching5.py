#!/usr/bin/env python3
"""
Comprehensive pattern matching fix using javalang parser.
This properly parses Java code and transforms pattern matching instanceof.
"""

import sys
import os
from pathlib import Path

try:
    import javalang
except ImportError:
    print("javalang not installed. Run: pip install javalang")
    sys.exit(1)

class PatternMatchingTransformer:
    def __init__(self):
        self.changes = []
    
    def transform(self, node, parent=None, parent_attr=None):
        """Recursively transform pattern matching in the AST."""
        if node is None:
            return
        
        # Handle InstanceOf with pattern variable
        if isinstance(node, javalang.tree.InstanceOf):
            if node.pattern and hasattr(node.pattern, 'name') and node.pattern.name:
                # Found pattern matching: expr instanceof Type var
                self.changes.append({
                    'node': node,
                    'parent': parent,
                    'parent_attr': parent_attr,
                    'expr': node.expression,
                    'type': node.pattern.type,
                    'var_name': node.pattern.name
                })
                # Remove the pattern (will be handled in source transformation)
                node.pattern = None
        
        # Recurse into children
        for attr_name in dir(node):
            if attr_name.startswith('_'):
                continue
            attr = getattr(node, attr_name)
            if isinstance(attr, javalang.tree.Node):
                self.transform(attr, node, attr_name)
            elif isinstance(attr, list):
                for i, item in enumerate(attr):
                    if isinstance(item, javalang.tree.Node):
                        self.transform(item, node, f"{attr_name}[{i}]")

def get_node_text(content, node):
    """Extract source text for a node using position info."""
    if hasattr(node, 'position') and node.position:
        line, col = node.position
        # This is approximate - javalang doesn't give exact end positions
        return None
    return None

def fix_file_with_javalang(filepath):
    """Fix pattern matching in a file using javalang."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return False
    
    try:
        tree = javalang.parse.parse(content)
    except Exception as e:
        print(f"Parse error in {filepath}: {e}")
        return False
    
    transformer = PatternMatchingTransformer()
    transformer.transform(tree)
    
    if not transformer.changes:
        return False
    
    # For now, we'll use a simpler approach: apply regex fixes based on the AST findings
    # But since javalang doesn't give exact positions, let's use a different strategy
    
    # Instead, let's use the tokenizer to get exact positions
    try:
        tokens = list(javalang.tokenizer.tokenize(content))
    except Exception as e:
        print(f"Tokenize error in {filepath}: {e}")
        return False
    
    # This is getting complex. Let's fall back to a more sophisticated regex approach
    # but guided by the AST findings
    return False

# Since the AST approach is complex without a pretty printer, let's use a better regex approach
# that processes the file as a whole with proper handling of nested structures

def fix_pattern_matching_comprehensive(content):
    """
    Comprehensive fix for all pattern matching instanceof patterns.
    Uses a state machine to track parentheses and brackets.
    """
    # First, let's handle the simple cases that my previous scripts missed
    # We'll do multiple passes
    
    # Pass 1: Fix ternary expressions with pattern matching (anywhere)
    # Pattern: expr instanceof Type var ? var : falseExpr
    def replace_ternary(match):
        full_match = match.group(0)
        expr = match.group(1)
        type_name = match.group(2)
        var_name = match.group(3)
        false_expr = match.group(4)
        return f"{expr} instanceof {type_name} ? ({type_name}) {expr} : {false_expr}"
    
    # This regex finds: expr instanceof Type var ? var : falseExpr
    # where expr doesn't contain unmatched parentheses
    ternary_pattern = re.compile(
        r'(\b(?:[a-zA-Z_$][a-zA-Z0-9_$]*(?:\.[a-zA-Z_$][a-zA-Z0-9_$]*)*(?:\([^)]*\))?))\s+instanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\?\s*\3\s*:\s*([^,\n;\)]+)'
    )
    content = ternary_pattern.sub(replace_ternary, content)
    
    # Pass 2: Fix if/else if/while conditions with pattern matching
    lines = content.split('\n')
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]
        
        # if (expr instanceof Type var)
        match = re.match(r'^(\s*)if\s*\((.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
            has_brace = line.rstrip().endswith('{')
            
            new_condition = f"{indent}if ({expr} instanceof {type_name})"
            if has_brace:
                new_condition += " {"
            new_lines.append(new_condition)
            
            if has_brace:
                new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
            else:
                i += 1
                if i < len(lines):
                    next_line = lines[i]
                    if next_line.strip() == '{':
                        new_lines.append(next_line)
                        new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                    else:
                        new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                        new_lines.append(next_line)
                        i += 1
                        continue
            i += 1
            continue
        
        # else if (expr instanceof Type var)
        match = re.match(r'^(\s*)else\s+if\s*\((.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
            has_brace = line.rstrip().endswith('{')
            
            new_condition = f"{indent}else if ({expr} instanceof {type_name})"
            if has_brace:
                new_condition += " {"
            new_lines.append(new_condition)
            
            if has_brace:
                new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
            else:
                i += 1
                if i < len(lines):
                    next_line = lines[i]
                    if next_line.strip() == '{':
                        new_lines.append(next_line)
                        new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                    else:
                        new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                        new_lines.append(next_line)
                        i += 1
                        continue
            i += 1
            continue
        
        # while (expr instanceof Type var)
        match = re.match(r'^(\s*)while\s*\((.+?)\binstanceof\s+([A-Z][a-zA-Z0-9_<>,?\[\]]*(?:\.[A-Z][a-zA-Z0-9_<>,?\[\]]*)*)\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\)\s*\{?\s*$', line)
        if match:
            indent = match.group(1)
            expr = match.group(2).strip()
            type_name = match.group(3)
            var_name = match.group(4)
            has_brace = line.rstrip().endswith('{')
            
            new_condition = f"{indent}while ({expr} instanceof {type_name})"
            if has_brace:
                new_condition += " {"
            new_lines.append(new_condition)
            
            if has_brace:
                new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
            else:
                i += 1
                if i < len(lines):
                    next_line = lines[i]
                    if next_line.strip() == '{':
                        new_lines.append(next_line)
                        new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                    else:
                        new_lines.append(f"{indent}    {type_name} {var_name} = ({type_name}) {expr};")
                        new_lines.append(next_line)
                        i += 1
                        continue
            i += 1
            continue
        
        # Variable declaration with pattern matching: Type var = expr instanceof Type var ? ...
        # This is rare but possible
        
        new_lines.append(line)
        i += 1
    
    content = '\n'.join(new_lines)
    
    # Pass 3: Fix pattern matching in complex conditions (with &&, ||)
    # Pattern: expr instanceof Type var && ...
    # This is tricky - we need to replace the pattern variable usage
    # For now, let's handle the case where pattern variable is used immediately after
    
    return content

import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    content = fix_pattern_matching_comprehensive(content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
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